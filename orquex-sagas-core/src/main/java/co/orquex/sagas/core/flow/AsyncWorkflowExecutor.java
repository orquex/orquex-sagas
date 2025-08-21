package co.orquex.sagas.core.flow;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
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
 * @see AbstractAsyncExecutable
 */
@Slf4j
public class AsyncWorkflowExecutor extends AbstractAsyncExecutable<ExecutionRequest> {

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

  /** {@inheritDoc} */
  @Override
  public void execute(ExecutionRequest request) {
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
      if (isExecutionResume(flow, correlationId, transaction)) return;
      // Otherwise, throw exception.
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
    this.publishStageEvent(transaction.getTransactionId(), initialStage, request);
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
   * @return true if the execution is resumed from a checkpoint, false if no resume is needed
   * @throws WorkflowException if resume is configured but checkpoint repository is not available,
   *     or if no checkpoint is found for the transaction
   */
  private boolean isExecutionResume(Flow flow, String correlationId, Transaction transaction) {
    // If already exists check if is resume from failure.
    final var resumeFromFailure = flow.configuration().resumeFromFailure();

    // Early return, if not resuming or in error, exit immediately
    if (!resumeFromFailure || !transaction.getStatus().equals(Status.ERROR)) {
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
            .findByTransactionId(transaction.getTransactionId())
            .orElseThrow(
                () ->
                    new WorkflowException(
                        "Flow '%s' with correlation id '%s' has no checkpoints to resume from."
                            .formatted(flowId, correlationId)));
    // Get checkpoint stage from flow's stages.
    final var stage = getStage(flow, checkpoint.stageId());
    final var request =
        new ExecutionRequest(flowId, correlationId, checkpoint.metadata(), checkpoint.payload());
    // Execute the stage from current checkpoint.
    this.publishStageEvent(transaction.getTransactionId(), stage, request);

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
