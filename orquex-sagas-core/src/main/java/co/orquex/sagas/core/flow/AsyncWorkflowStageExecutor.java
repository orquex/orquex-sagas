package co.orquex.sagas.core.flow;

import static java.util.Objects.isNull;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

/**
 * This class executes a workflow stage in a choreography manner.
 *
 * @see AbstractAsyncExecutable
 */
@Slf4j
public class AsyncWorkflowStageExecutor extends AbstractAsyncExecutable<Checkpoint> {

  private final WorkflowEventPublisher workflowEventPublisher;

  public AsyncWorkflowStageExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
    this.workflowEventPublisher = workflowEventPublisher;
  }

  /**
   * Execute the workflow stage after receiving a checkpoint.
   *
   * @param checkpoint the checkpoint notified by the event manager.
   */
  @Override
  public void execute(Checkpoint checkpoint) {
    try {
      if (isNull(checkpoint.outgoing())) {
        log.warn(
            "Flow '{}' with correlation ID '{}' has been completed.",
            checkpoint.flowId(),
            checkpoint.correlationId());
        return;
      }

      if (!transactionRepository.existsByFlowIdAndCorrelationId(
          checkpoint.flowId(), checkpoint.correlationId())) {
        final var message =
            "Transaction not found by flowId '%s' and correlation ID '%s'."
                .formatted(checkpoint.flowId(), checkpoint.correlationId());
        publishError(checkpoint, message);
        return;
      }

      final var optionalFlow = flowRepository.findById(checkpoint.flowId());
      if (optionalFlow.isEmpty()) {
        final var message =
            "Flow not found by '%s' when executing checkpoint stage with correlation ID '%s'."
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
              flow.metadata(),
              checkpoint.response());
      final var stageRequest = getStageRequest(checkpoint.transactionId(), stage, executionRequest);
      log.debug(
          "Executing next stage '{}' of flow '{}' with correlation ID '{}'.",
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
    final var error =
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
