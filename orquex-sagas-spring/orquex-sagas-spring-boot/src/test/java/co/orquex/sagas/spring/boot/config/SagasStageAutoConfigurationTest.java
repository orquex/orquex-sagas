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
@ContextConfiguration(classes = {SagasStageAutoConfiguration.class, MockConfiguration.class})
@TestPropertySource(properties = {"orquex.sagas.spring.stage.enabled=true"})
class SagasStageAutoConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadStageConfiguration() {
    assertThat(applicationContext.getBean("defaultStageExecutor")).isNotNull();
    assertThat(applicationContext.getBean("defaultAsyncStageExecutor")).isNotNull();
    assertThat(applicationContext.getBean("defaultTaskImplementationRegistry")).isNotNull();
    assertThat(applicationContext.getBean("defaultTaskExecutorRegistry")).isNotNull();
    assertThat(applicationContext.getBean("defaultStageExecutorRegistry")).isNotNull();
    assertThat(applicationContext.getBean("defaultWorkflowEventPublisher")).isNotNull();
    assertThat(applicationContext.getBean("defaultEventManagerFactory")).isNotNull();
  }
}
