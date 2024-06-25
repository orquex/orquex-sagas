package co.orquex.sagas.sample.cs.api.event;

import static java.util.Objects.nonNull;

import co.orquex.sagas.core.flow.WorkflowStageExecutor;
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
public class CheckpointEventListenerHandler {

  private final WorkflowStageExecutor workflowStageExecutor;
  private final ObjectMapper objectMapper;

  @Async
  @EventListener(condition = "#event.message.status.name() == 'COMPLETED'")
  public void handleCheckpointCompleted(CheckpointEventMessage event)
      throws JsonProcessingException {
    final var checkpoint = event.getMessage();
    log.trace("Handling checkpoint completed: {}", checkpoint);
    if (nonNull(checkpoint.outgoing())) {
      workflowStageExecutor.execute(checkpoint);
    } else {
      log.info(
          "Workflow finished with response {}",
          objectMapper.writeValueAsString(checkpoint.response()));
    }
  }

  @Async
  @EventListener(condition = "#event.message.status.name() == 'ERROR'")
  public void handleCheckpointError(CheckpointEventMessage event) {
    log.error("Handling checkpoint error: {}", event.getError().message());
  }

  @Async
  @EventListener(condition = "#event.message.status.name() == 'CANCELED'")
  public void handleCheckpointCanceled(CheckpointEventMessage event) {
    log.warn("Handling checkpoint canceled: {}", event);
  }

  @Async
  @EventListener(condition = "#event.message.status.name() == 'IN_PROGRESS'")
  public void handleCheckpointInProgress(CheckpointEventMessage event) {
    log.trace("Handling checkpoint in progress: {}", event);
  }
}
