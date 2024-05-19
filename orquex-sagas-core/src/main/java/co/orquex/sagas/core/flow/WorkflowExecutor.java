package co.orquex.sagas.core.flow;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowExecutor extends AbstractWorkflowExecutor<ExecutionRequest> {

  private final EventManager<StageRequest> stageRequestEventManager;

  public WorkflowExecutor(
      EventManager<StageRequest> stageRequestEventManager,
      FlowRepository flowRepository,
      TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.stageRequestEventManager = stageRequestEventManager;
  }

  @Override
  public void execute(ExecutionRequest request) {
    // Check executionRequest is not null.
    if (request == null) throw new WorkflowException("execution executionRequest required");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    log.debug(
        "Executing workflow '{}' with correlation ID '{}'", flow.name(), request.correlationId());
    // Check transaction already exists by correlation id.
    if (transactionRepository.existByFlowIdAndCorrelationId(flow.id(), request.correlationId())) {
      throw new WorkflowException(
          "flow '%s' with correlation id '%s' has already been initiated"
              .formatted(flow.name(), request.correlationId()));
    }
    // Register the transaction.
    final var transaction = saveTransaction(flow, request);
    // Get initial stage from stages.
    final var stage = getStage(flow, flow.initialStage());
    // Start execution of the workflow.
    stageRequestEventManager.send(getStageRequest(transaction, stage, request));
  }

  private Transaction saveTransaction(Flow flow, ExecutionRequest request) {
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
