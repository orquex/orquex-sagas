package co.orquex.sagas.sample.cs.api.event;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Listens for checkpoint messages from the message broker. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointKafkaListener {

  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  @KafkaListener(id = "checkpoint-listener", topics = "coffee.shop.stage.checkpoint")
  public void listen(String message) throws JsonProcessingException {
    final var eventMessage =
        objectMapper.readValue(message, new TypeReference<EventMessage<Checkpoint>>() {});
    final var checkpoint = eventMessage.message();
    log.debug("Checkpoint received {}", checkpoint);
    log.trace(
        "Received checkpoint for '{}' with correlation ID '{}' in stage '{}' and status '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName(),
        checkpoint.status());
    eventPublisher.publishEvent(new CheckpointEventMessage(eventMessage));
  }
}
