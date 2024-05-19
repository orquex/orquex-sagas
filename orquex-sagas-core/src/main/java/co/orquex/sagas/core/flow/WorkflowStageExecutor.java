package co.orquex.sagas.core.flow;

import static java.util.Objects.isNull;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowStageExecutor extends AbstractWorkflowExecutor<Checkpoint> {

  private final EventManager<StageRequest> stageRequestEventManager;

  public WorkflowStageExecutor(
      EventManager<StageRequest> stageRequestEventManager,
      FlowRepository flowRepository,
      TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.stageRequestEventManager = stageRequestEventManager;
  }

  @Override
  public void execute(Checkpoint checkpoint) {

    if (isNull(checkpoint.outgoing())) {
      log.warn(
          "Flow '{}' with correlation ID '{}' has been completed",
          checkpoint.flowId(),
          checkpoint.correlationId());
      return;
    }

    if (!transactionRepository.existByFlowIdAndCorrelationId(
        checkpoint.flowId(), checkpoint.correlationId())) {
      throw new WorkflowException(
          "Transaction not found by flowId: "
              + checkpoint.flowId()
              + " and correlationId: "
              + checkpoint.correlationId());
    }
    final var flow =
        flowRepository
            .findById(checkpoint.flowId())
            .orElseThrow(() -> new WorkflowException("workflow not found"));
    // Get the next stage from checkpoint
    final var stage = getStage(flow, checkpoint.outgoing());
    final var executionRequest =
        new ExecutionRequest(
            checkpoint.flowId(),
            checkpoint.correlationId(),
            checkpoint.metadata(),
            checkpoint.response());
    final var stageRequest =
        StageRequest.builder()
            .transactionId(checkpoint.transactionId())
            .stage(stage)
            .executionRequest(executionRequest)
            .build();
    log.debug("Executing next stage '{}'", stage.getName());
    // Continue execution of the workflow.
    stageRequestEventManager.send(stageRequest);
  }
}
