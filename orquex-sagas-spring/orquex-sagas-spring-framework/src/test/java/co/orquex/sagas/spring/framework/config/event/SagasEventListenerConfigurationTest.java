package co.orquex.sagas.spring.framework.config.event;

import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.spring.framework.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SagasAsyncWorkflowConfiguration.class,
      SagasAsyncStageConfiguration.class,
      SagasWorkflowEventPublisherConfiguration.class,
      SagasRegistryConfiguration.class,
      MockRepositoryConfiguration.class,
      SagasEventListenerConfiguration.class,
      SagasResilienceConfiguration.class
    })
class SagasEventListenerConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadTaskConfiguration() {
    assertThat(applicationContext.getBean("defaultStageEventListener")).isNotNull();
    assertThat(applicationContext.containsBean("defaultCompensationEventListener")).isFalse();
  }
}
