package co.orquex.sagas.sample.cs.service.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointListener implements EventListener<Checkpoint> {

    private static final String checkpointTopic = "coffee.shop.stage.checkpoint";
    private static final String errorTopic = "coffee.shop.stage.error";
    private final KafkaTemplate<String, EventMessage<Checkpoint>> kafkaTemplate;

    @Override
    public void onMessage(EventMessage<Checkpoint> message) {
        final var checkpoint = message.getMessage();
        log.debug(
                "Received checkpoint for '{}' with correlation ID '{}' in stage '{}' and status '{}'",
                checkpoint.flowId(), checkpoint.correlationId(), checkpoint.incoming().getName(),
                checkpoint.status());
        kafkaTemplate.send(checkpointTopic, message);
    }

    @Override
    public void onError(EventMessage<Checkpoint> message) {
        final var checkpoint = message.getMessage();
        log.error("Error received for '{}' with correlation ID '{}' in stage '{}'",
                checkpoint.flowId(), checkpoint.correlationId(), checkpoint.incoming().getName());
        kafkaTemplate.send(errorTopic, message);
    }
}
