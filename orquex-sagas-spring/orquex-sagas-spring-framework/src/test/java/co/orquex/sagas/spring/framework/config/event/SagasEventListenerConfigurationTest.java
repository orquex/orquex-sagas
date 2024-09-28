package co.orquex.sagas.spring.framework.config.event;

import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.spring.framework.config.MockConfiguration;
import co.orquex.sagas.spring.framework.config.SagasStageConfiguration;
import co.orquex.sagas.spring.framework.config.SagasWorkflowConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SagasStageConfiguration.class,
      SagasWorkflowConfiguration.class,
      SagasEventListenerConfiguration.class,
      MockConfiguration.class
    })
class SagasEventListenerConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadTaskConfiguration() {
    assertThat(applicationContext.getBean("defaultCheckpointEventListener")).isNotNull();
    assertThat(applicationContext.getBean("defaultCheckpointEventListenerHandler")).isNotNull();
  }
}
