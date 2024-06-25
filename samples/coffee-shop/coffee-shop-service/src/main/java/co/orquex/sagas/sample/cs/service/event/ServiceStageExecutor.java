package co.orquex.sagas.sample.cs.service.event;

import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.stage.StageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceStageExecutor {

  private final StageExecutor stageExecutor;
  private final ObjectMapper objectMapper;

  @KafkaListener(id = "service-listener", topics = "coffee.shop.stage.service")
  public void listen(String request) throws JsonProcessingException {
    final var stageRequest = objectMapper.readValue(request, StageRequest.class);
    stageExecutor.execute(stageRequest);
  }
}
