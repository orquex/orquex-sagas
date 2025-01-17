package co.orquex.sagas.spring.framework.config.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Compensation;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the required Event Listener beans. */
@Configuration
public class SagasEventListenerConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"defaultStageEventListener"})
  public EventListener<StageRequest> defaultStageEventListener(
      Registry<StageExecutor> stageExecutorRegistry) {
    return new DefaultStageEventListener(stageExecutorRegistry);
  }

  @Bean
  @ConditionalOnProperty(name = "orquex.sagas.spring.compensation.enabled", havingValue = "true")
  public EventListener<Compensation> defaultCompensationEventListener(
      CompensationRepository compensationRepository) {
    return new DefaultCompensationEventListener(compensationRepository);
  }
}
