package co.orquex.sagas.core.flow;

import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowExecutor implements Executable<ExecutionRequest> {

  private final Executable<StageRequest> executableStage;
  private final FlowRepository flowRepository;
  private final TransactionRepository transactionRepository;

  @Override
  public void execute(ExecutionRequest request) {
    // Check executionRequest is not null.
    if (request == null) throw new WorkflowException("execution executionRequest required");
    // Get flow by id.
    final var flow = getFlow(request.flowId());
    // Check transaction already exists by correlation id.
    if (transactionRepository.existByFlowIdAndCorrelationId(flow.id(), request.correlationId())) {
      throw new WorkflowException(
          "flow '%s' with correlation id '%s' has already been initiated"
              .formatted(flow.name(), request.correlationId()));
    }
    // Register the transaction.
    final var transaction = saveTransaction(flow, request);
    // Get initial stage from stages.
    final var stage = flow.stages().get(flow.initialStage());
    // Start execution of the workflow.
    executableStage.execute(getStageRequest(transaction, stage, request));
  }

  public StageRequest getStageRequest(
      Transaction transaction, Stage stage, ExecutionRequest request) {
    return StageRequest.builder()
        .transactionId(transaction.getTransactionId())
        .stage(stage)
        .executionRequest(request)
        .build();
  }

  private Flow getFlow(String flowId) {
    return flowRepository
        .findById(flowId)
        .orElseThrow(() -> new WorkflowException("workflow not found"));
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
