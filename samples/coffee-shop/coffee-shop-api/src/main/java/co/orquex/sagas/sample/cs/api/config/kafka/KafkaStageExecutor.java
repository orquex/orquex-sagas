package co.orquex.sagas.sample.cs.api.config.kafka;

import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStageExecutor implements StageExecutor {

  public static final String TOPIC_PARAM = "topic";
  private final KafkaTemplate<String, StageRequest> template;

  @Override
  public StageResponse execute(StageRequest stageRequest) {
    // Assuming that values never comes null
    final var stage = stageRequest.stage();
    final var config = stage.getConfiguration();
    if (config.parameters().containsKey(TOPIC_PARAM)) {
      // Assuming that the topic is string value
      final var topic = config.parameters().get(TOPIC_PARAM).toString();
      template
          .send(topic, stageRequest.transactionId(), stageRequest)
          .thenAccept(result -> log.debug("Stage '{}' sent to topic '{}'", stage.getName(), topic));
    }
    return new StageResponse(stageRequest.transactionId());
  }

  @Override
  public String getKey() {
    return "kafka-executor";
  }
}
