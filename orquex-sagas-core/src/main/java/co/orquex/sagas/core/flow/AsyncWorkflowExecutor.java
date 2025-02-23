package co.orquex.sagas.core.flow;


import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * This class executes a workflow in a choreography manner.
 *
 * @see AbstractWorkflowExecutor
 */
@Slf4j
public class AsyncWorkflowExecutor extends AbstractAsyncExecutable<ExecutionRequest> {

  private final WorkflowEventPublisher workflowEventPublisher;

  public AsyncWorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.workflowEventPublisher = workflowEventPublisher;
  }

  @Override
  public void execute(ExecutionRequest request) {
    // Check executionRequest isn't null.
    if (request == null) throw new WorkflowException("Execution request required.");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    log.debug(
        "Executing workflow '{}' with correlation ID '{}'", flow.name(), request.correlationId());
    // Check transaction already exists by correlation id.
    if (transactionRepository.existsByFlowIdAndCorrelationId(flow.id(), request.correlationId())) {
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' has already been initiated."
              .formatted(flow.id(), request.correlationId()));
    }
    // Get initial stage from stages.
    final var stage = getStage(flow, flow.initialStage());
    // Merge request and flow metadata.
    request = request.mergeMetadata(flow.metadata());
    // Register the transaction.
    final var transaction = initializeTransaction(flow, request);
    // Start execution of the workflow.
    final var stageEventMessage =
        new EventMessage<>(getStageRequest(transaction.getTransactionId(), stage, request));
    workflowEventPublisher.publish(stageEventMessage);
  }
}
