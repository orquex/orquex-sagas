package co.orquex.sagas.core.flow;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.api.repository.CheckpointRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;

/**
 * Asynchronous workflow executor that orchestrates saga workflows using event-driven choreography.
 * Handles workflow initiation, transaction management, and resume-from-failure capabilities through
 * checkpoint recovery. Each workflow stage is executed asynchronously via event publishing.
 *
 * @see AbstractWorkflowExecutor
 * @see Executable
 */
@Slf4j
public class AsyncWorkflowExecutor extends AbstractWorkflowExecutor
    implements Executable<ExecutionRequest, String> {

  private final WorkflowEventPublisher workflowEventPublisher;

  /**
   * Constructs an AsyncWorkflowExecutor with the required dependencies.
   *
   * @param workflowEventPublisher the event publisher for workflow events
   * @param flowRepository the repository for flow operations
   * @param transactionRepository the repository for transaction operations
   */
  public AsyncWorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.workflowEventPublisher = workflowEventPublisher;
  }

  /**
   * Constructs an AsyncWorkflowExecutor with the required dependencies including checkpoint
   * repository.
   *
   * @param workflowEventPublisher the event publisher for workflow events
   * @param flowRepository the repository for flow operations
   * @param transactionRepository the repository for transaction operations
   * @param checkpointRepository the repository for checkpoint operations, enabling resume from
   *     failure functionality
   */
  public AsyncWorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository,
      CheckpointRepository checkpointRepository) {
    super(flowRepository, transactionRepository, checkpointRepository);
    this.workflowEventPublisher = workflowEventPublisher;
  }

  /**
   * Executes a workflow asynchronously based on the provided {@link ExecutionRequest}.
   *
   * <p>This method orchestrates the following steps:
   *
   * <ul>
   *   <li>Validates the execution request is not null.
   *   <li>Retrieves the workflow definition by flow ID.
   *   <li>Checks for an existing transaction with the same correlation ID:
   *       <ul>
   *         <li>If a transaction exists and is eligible for resume-from-failure, resumes execution
   *             from the last checkpoint.
   *         <li>If a transaction exists but cannot be resumed, throws a {@link WorkflowException}.
   *       </ul>
   *   <li>Initializes a new transaction if none exists.
   *   <li>Merges request metadata with flow metadata.
   *   <li>Publishes an event to start execution of the initial workflow stage.
   * </ul>
   *
   * @param request the execution request containing flow ID, correlation ID, metadata, and payload
   * @return the transaction ID for the initiated or resumed workflow
   * @throws WorkflowException if the request is null, the flow or transaction is invalid, or
   *     resume-from-failure is misconfigured
   */
  @Override
  public String execute(ExecutionRequest request) {
    // Check executionRequest isn't null.
    if (request == null) throw new WorkflowException("Execution request required.");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    final var correlationId = request.correlationId();
    final var flowId = flow.id();
    log.debug("Executing workflow '{}' with correlation ID '{}'", flow.name(), correlationId);
    // Check transaction already exists by correlation id and is in progress status.
    final var optTransaction =
        transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId);
    if (optTransaction.isPresent()) {
      final var transaction = optTransaction.get();
      // If resume from failure is enabled, check if execution can resume from a previous failure.
      if (isExecutionResume(flow, correlationId, transaction)) return transaction.transactionId();
      // Otherwise, throw an exception.
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' has already been initiated."
              .formatted(flowId, correlationId));
    }
    // Get initial stage from stages.
    final var initialStage = getStage(flow, flow.initialStage());
    // Merge request and flow metadata.
    request = request.mergeMetadata(flow.metadata());
    // Register the transaction.
    final var transaction = initializeTransaction(flow, request);
    // Start execution of the workflow.
    this.publishStageEvent(transaction.transactionId(), initialStage, request);
    // Return the transaction id.
    return transaction.transactionId();
  }

  /**
   * Determines if the workflow execution should resume from a previous failure. This method checks
   * if the flow configuration allows resume from failure and validates the required checkpoint
   * repository. If resumption is allowed, it retrieves the last checkpoint and publishes the
   * corresponding stage event to continue execution.
   *
   * @param flow the workflow flow definition
   * @param correlationId the correlation identifier for the transaction
   * @param transaction the existing transaction to potentially resume
   * @return true if the execution is resumed from a checkpoint, false if no resumed is needed
   * @throws WorkflowException if resume is configured, but the checkpoint repository is not
   *     available, or if no checkpoint is found for the transaction
   */
  private boolean isExecutionResume(Flow flow, String correlationId, Transaction transaction) {
    // If already exists check if is resume from failure.
    final var resumeFromFailure = flow.configuration().resumeFromFailure();

    // Early return, if not resuming or in error, exit immediately
    if (!resumeFromFailure || !transaction.status().equals(Status.ERROR)) {
      return false;
    }

    final var flowId = flow.id();

    if (checkpointRepository == null) {
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' cannot be resumed from failure because checkpoint repository is not provided."
              .formatted(flowId, correlationId));
    }

    // If so, then continue from the last stage that has failed.
    final var checkpoint =
        checkpointRepository
            .findByTransactionId(transaction.transactionId())
            .orElseThrow(
                () ->
                    new WorkflowException(
                        "Flow '%s' with correlation id '%s' has no checkpoints to resume from."
                            .formatted(flowId, correlationId)));
    // Get checkpoint stage from flow's stages.
    final var stage = getStage(flow, checkpoint.stageId());
    final var request =
        new ExecutionRequest(flowId, correlationId, checkpoint.metadata(), checkpoint.payload());
    // Execute the stage from the current checkpoint.
    this.publishStageEvent(transaction.transactionId(), stage, request);

    return true;
  }

  /**
   * Publishes a stage event to initiate the execution of a specific workflow stage. This method
   * creates a stage request from the provided parameters, wraps it in an event message, and
   * publishes it through the workflow event publisher.
   *
   * @param transactionId the unique identifier of the transaction
   * @param stage the stage to be executed
   * @param request the execution request containing payload and metadata
   */
  private void publishStageEvent(String transactionId, Stage stage, ExecutionRequest request) {
    final var stageRequest = getStageRequest(transactionId, stage, request);
    final var stageEventMessage = new EventMessage<>(stageRequest);
    workflowEventPublisher.publish(stageEventMessage);
  }
}
