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
@ContextConfiguration(classes = { SagasWorkflowAutoConfiguration.class, MockConfiguration.class})
@TestPropertySource(properties = {"orquex.sagas.spring.workflow.enabled=true"})
class SagasWorkflowAutoConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadStageConfiguration() {
    assertThat(applicationContext.getBean("workflowExecutor")).isNotNull();
    assertThat(applicationContext.getBean("workflowStageExecutor")).isNotNull();
  }
}
