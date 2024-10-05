package co.orquex.sagas.core.flow;

import static co.orquex.sagas.domain.utils.Maps.merge;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * This class executes a workflow in a choreography manner.
 *
 * @see AbstractWorkflowExecutor
 */
@Slf4j
public class WorkflowExecutor extends AbstractWorkflowExecutor<ExecutionRequest> {

  public WorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(workflowEventPublisher, flowRepository, transactionRepository);
  }

  @Override
  public void execute(ExecutionRequest request) {
    // Check executionRequest isn't null.
    if (request == null) throw new WorkflowException("Execution request required");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    log.debug(
        "Executing workflow '{}' with correlation ID '{}'", flow.name(), request.correlationId());
    // Check transaction already exists by correlation id.
    if (transactionRepository.existsByFlowIdAndCorrelationId(flow.id(), request.correlationId())) {
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' has already been initiated"
              .formatted(flow.name(), request.correlationId()));
    }
    // Get initial stage from stages.
    final var stage = getStage(flow, flow.initialStage());
    // Merge request, flow and stage metadata.
    request = request.mergeMetadata(merge(flow.metadata(), stage.getMetadata()));
    // Register the transaction.
    final var transaction = saveTransaction(flow, request);
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
