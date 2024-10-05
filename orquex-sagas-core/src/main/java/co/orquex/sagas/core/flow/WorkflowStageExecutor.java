package co.orquex.sagas.core.flow;

import static co.orquex.sagas.domain.utils.Maps.merge;
import static java.util.Objects.isNull;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowStageExecutor extends AbstractWorkflowExecutor<Checkpoint> {

  public WorkflowStageExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(workflowEventPublisher, flowRepository, transactionRepository);
  }

  @Override
  public void execute(Checkpoint checkpoint) {
    try {
      if (isNull(checkpoint.outgoing())) {
        log.warn(
            "Flow '{}' with correlation ID '{}' has been completed",
            checkpoint.flowId(),
            checkpoint.correlationId());
        return;
      }

      if (!transactionRepository.existsByFlowIdAndCorrelationId(
          checkpoint.flowId(), checkpoint.correlationId())) {
        final var message =
            "Transaction not found by flowId '%s' and correlation ID '%s'"
                .formatted(checkpoint.flowId(), checkpoint.correlationId());
        publishError(checkpoint, message);
        return;
      }

      final var optionalFlow = flowRepository.findById(checkpoint.flowId());
      if (optionalFlow.isEmpty()) {
        final var message =
            "Flow not found by '%s' when executing checkpoint stage with correlation ID '%s'"
                .formatted(checkpoint.flowId(), checkpoint.correlationId());
        publishError(checkpoint, message);
        return;
      }

      final var flow = optionalFlow.get();
      // Get the next stage from checkpoint
      final var stage = getStage(flow, checkpoint.outgoing());
      final var executionRequest =
          new ExecutionRequest(
              checkpoint.flowId(),
              checkpoint.correlationId(),
              merge(flow.metadata(), stage.getMetadata()),
              checkpoint.response());
      final var stageRequest =
          StageRequest.builder()
              .transactionId(checkpoint.transactionId())
              .stage(stage)
              .executionRequest(executionRequest)
              .build();
      log.debug(
          "Executing next stage '{}' of flow '{}' with correlation ID '{}'",
          stage.getName(),
          checkpoint.flowId(),
          checkpoint.correlationId());
      // Continue the execution of the workflow.
      workflowEventPublisher.publish(new EventMessage<>(stageRequest));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      publishError(checkpoint, e.getMessage());
    }
  }

  private void publishError(Checkpoint checkpoint, String message) {
    Error error =
        Error.builder()
            .transactionId(checkpoint.transactionId())
            .flowId(checkpoint.flowId())
            .correlationId(checkpoint.correlationId())
            .timestamp(Instant.now())
            .message(message)
            .build();
    workflowEventPublisher.publish(new EventMessage<>(error));
  }
}
