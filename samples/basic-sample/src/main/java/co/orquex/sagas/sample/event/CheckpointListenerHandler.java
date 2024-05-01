package co.orquex.sagas.sample.event;

import static java.util.Objects.nonNull;

import co.orquex.sagas.core.flow.WorkflowStageExecutor;
import co.orquex.sagas.domain.transaction.Checkpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointListenerHandler {

  private final WorkflowStageExecutor workflowStageExecutor;
  private final ObjectMapper objectMapper;

  @Async
  @EventListener(condition = "#checkpoint.status.name() == 'COMPLETED'")
  public void handleCheckpointCompleted(Checkpoint checkpoint) throws JsonProcessingException {
    log.trace("Handling checkpoint completed: {}", checkpoint);
    if (nonNull(checkpoint.outgoing())) workflowStageExecutor.execute(checkpoint);
    // For testing purpose printing the result
    else
      log.info(
          "Workflow finished with response {}",
          objectMapper.writeValueAsString(checkpoint.response()));
  }

  @Async
  @EventListener(condition = "#checkpoint.status.name() == 'ERROR'")
  public void handleCheckpointError(Checkpoint checkpoint) {
    log.error("Handling checkpoint error: {}", checkpoint);
  }

  @Async
  @EventListener(condition = "#checkpoint.status.name() == 'CANCELED'")
  public void handleCheckpointCanceled(Checkpoint checkpoint) {
    log.warn("Handling checkpoint canceled: {}", checkpoint);
  }

  @Async
  @EventListener(condition = "#checkpoint.status.name() == 'IN_PROGRESS'")
  public void handleCheckpointInProgress(Checkpoint checkpoint) {
    log.trace("Handling checkpoint in progress: {}", checkpoint);
  }
}
