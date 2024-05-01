package co.orquex.sagas.sample.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@RequiredArgsConstructor
public class CheckpointListener implements EventListener<Checkpoint> {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void onMessage(EventMessage<Checkpoint> message) {
    final var checkpoint = message.getMessage();
    log.trace(
        "Received checkpoint for '{}' with correlation ID '{}' in stage '{}' and status '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName(),
        checkpoint.status());
    eventPublisher.publishEvent(checkpoint);
  }

  @Override
  public void onError(EventMessage<Checkpoint> message) {
    final var checkpoint = message.getMessage();
    log.error(
        "Error received for '{}' with correlation ID '{}' in stage '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName());
    eventPublisher.publishEvent(message.getError());
    // Check with circuit breaker
    // Check retry
    // Check timeout
  }
}
