package co.orquex.sagas.core.flow;

import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractWorkflowExecutor<T> implements Executable<T> {

  protected final FlowRepository flowRepository;
  protected final TransactionRepository transactionRepository;

  protected Flow getFlow(String flowId) {
    return flowRepository
        .findById(flowId)
        .orElseThrow(() -> new WorkflowException(String.format("Flow '%s' not found.", flowId)));
  }

  protected Stage getStage(Flow flow, String stageId) {
    final var stages = flow.stages();
    if (stages.containsKey(stageId)) {
      return stages.get(stageId);
    }
    throw new WorkflowException(
        String.format("Stage '%s' not found in flow '%s'.", stageId, flow.id()));
  }

  protected StageRequest getStageRequest(
      Transaction transaction, Stage stage, ExecutionRequest request) {
    return StageRequest.builder()
        .transactionId(transaction.getTransactionId())
        .stage(stage)
        .executionRequest(request)
        .build();
  }
}
