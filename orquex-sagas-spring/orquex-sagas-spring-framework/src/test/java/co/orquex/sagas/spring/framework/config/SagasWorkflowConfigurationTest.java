package co.orquex.sagas.spring.framework.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SagasWorkflowConfiguration.class,
      SagasStageConfiguration.class,
      SagasRegistryConfiguration.class,
      SagasWorkflowEventPublisherConfiguration.class,
      MockRepositoryConfiguration.class
    })
class SagasWorkflowConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadWorkflowConfiguration() {
    assertThat(applicationContext.getBean("workflowExecutor")).isNotNull();
    assertThat(applicationContext.getBean("defaultStageExecutor")).isNotNull();
    assertThat(applicationContext.getBean("flowRepository")).isNotNull();
    assertThat(applicationContext.getBean("transactionRepository")).isNotNull();
    assertThat(applicationContext.getBean("workflowExecutorService")).isNotNull();
  }
}
