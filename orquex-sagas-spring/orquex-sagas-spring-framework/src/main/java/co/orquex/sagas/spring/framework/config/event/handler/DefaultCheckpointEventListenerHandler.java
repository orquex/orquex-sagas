package co.orquex.sagas.spring.framework.config.event.handler;

import static java.util.Objects.nonNull;

import co.orquex.sagas.core.flow.AsyncWorkflowStageExecutor;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link CheckpointEventListenerHandler} that handles the different
 * status of a {@link Checkpoint} and executes the next stage when there's an outgoing and the
 * previous one was completed.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCheckpointEventListenerHandler implements CheckpointEventListenerHandler {

  private final AsyncWorkflowStageExecutor workflowStageExecutor;

  public void handle(Checkpoint checkpoint) {
    log.trace(
        "Listener Handler received checkpoint for '{}' and correlation ID '{}' at stage '{}' with status '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName(),
        checkpoint.status());
    switch (checkpoint.status()) {
      case COMPLETED -> handleCheckpointCompleted(checkpoint);
      case ERROR -> handleCheckpointError(checkpoint);
      case CANCELED -> handleCheckpointCanceled(checkpoint);
      case IN_PROGRESS -> handleCheckpointInProgress(checkpoint);
    }
  }

  private void handleCheckpointCompleted(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
    if (nonNull(checkpoint.outgoing())) {
      workflowStageExecutor.execute(checkpoint);
    } else {
      log.info(
          "Workflow '{}' with correlation id '{}' has been completed",
          checkpoint.flowId(),
          checkpoint.correlationId());
    }
  }

  private void handleCheckpointError(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
  }

  private void handleCheckpointCanceled(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
  }

  private void handleCheckpointInProgress(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
  }

  private String getCheckpointStatus(Checkpoint checkpoint) {
    return "Handling checkpoint '%s' for '%s' in '%s' with correlation ID '%s'"
        .formatted(
            checkpoint.status().name(),
            checkpoint.incoming().getName(),
            checkpoint.flowId(),
            checkpoint.correlationId());
  }
}
