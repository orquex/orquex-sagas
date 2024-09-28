package co.orquex.sagas.spring.framework.config.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.spring.framework.config.event.handler.CheckpointEventListenerHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultCheckpointEventListener implements EventListener<Checkpoint> {

  private final CheckpointEventListenerHandler handler;

  @Override
  public void onMessage(EventMessage<Checkpoint> event) {
    final var checkpoint = event.message();
    log.trace(
        "Received checkpoint for '{}' and correlation ID '{}' at stage '{}' with status '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName(),
        checkpoint.status());
    handler.handle(checkpoint);
  }

  @Override
  public void onError(EventMessage<Checkpoint> event) {
    final var checkpoint = event.message();
    log.error(
        "Error received for '{}' with correlation ID '{}' at stage '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName());
    handler.handle(checkpoint);
  }
}
