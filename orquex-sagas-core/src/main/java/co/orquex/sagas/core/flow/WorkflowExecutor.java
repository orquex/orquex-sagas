package co.orquex.sagas.core.flow;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowExecutor extends AbstractWorkflowExecutor<ExecutionRequest> {

  private final WorkflowEventPublisher workflowEventPublisher;

  public WorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.workflowEventPublisher = workflowEventPublisher;
  }

  @Override
  public void execute(final ExecutionRequest request) {
    // Check executionRequest is not null.
    if (request == null) throw new WorkflowException("Execution request required");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    log.debug(
        "Executing workflow '{}' with correlation ID '{}'", flow.name(), request.correlationId());
    // Check transaction already exists by correlation id.
    if (transactionRepository.existByFlowIdAndCorrelationId(flow.id(), request.correlationId())) {
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' has already been initiated"
              .formatted(flow.name(), request.correlationId()));
    }
    // Register the transaction.
    final var transaction = saveTransaction(flow, request);
    // Get initial stage from stages.
    final var stage = getStage(flow, flow.initialStage());
    // Start execution of the workflow.
    final var stageEventMessage = new EventMessage<>(getStageRequest(transaction, stage, request));
    workflowEventPublisher.publish(stageEventMessage);
  }

  private Transaction saveTransaction(final Flow flow, final ExecutionRequest request) {
    return transactionRepository.save(
        Transaction.builder()
            .transactionId(UUID.randomUUID().toString())
            .flowId(flow.id())
            .correlationId(request.correlationId())
            .data(request)
            .status(Status.IN_PROGRESS)
            .startedAt(Instant.now())
            .expiresAt(Instant.now().plus(flow.configuration().timeout()))
            .build());
  }
}
