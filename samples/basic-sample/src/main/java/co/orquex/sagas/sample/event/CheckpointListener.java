package co.orquex.sagas.sample.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointListener implements EventListener<Checkpoint> {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void onMessage(EventMessage<Checkpoint> message) {
    final var checkpoint = message.message();
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
    final var checkpoint = message.message();
    log.error(
        "Error received for '{}' with correlation ID '{}' in stage '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName());
    eventPublisher.publishEvent(message.error());
    // Check with circuit breaker
    // Check retry
    // Check timeout
  }
}
