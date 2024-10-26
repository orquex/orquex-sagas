package co.orquex.sagas.spring.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SagasAsyncStageConfiguration.class,
      SagasWorkflowEventPublisherConfiguration.class,
      SagasRegistryConfiguration.class,
      MockRepositoryConfiguration.class
    })
class SagasAsyncStageConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadStageConfiguration() {
    assertThat(applicationContext.getBean("defaultAsyncStageExecutor")).isNotNull();
    assertThat(applicationContext.getBean("defaultTaskExecutorRegistry")).isNotNull();
    assertThat(applicationContext.getBean("taskRepository")).isNotNull();
    assertThat(applicationContext.getBean("defaultWorkflowEventPublisher")).isNotNull();
  }
}
