package co.orquex.sagas.spring.boot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SagasEventListenerAutoConfiguration.class,
      SagasWorkflowAutoConfiguration.class,
      SagasStageAutoConfiguration.class,
      MockConfiguration.class
    })
@TestPropertySource(properties = {"orquex.sagas.spring.event.enabled=true"})
class SagasEventListenerAutoConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadEventListenerConfiguration() {
    assertThat(applicationContext.getBean("defaultCheckpointEventListener")).isNotNull();
    assertThat(applicationContext.getBean("defaultStageEventListener")).isNotNull();
  }
}
