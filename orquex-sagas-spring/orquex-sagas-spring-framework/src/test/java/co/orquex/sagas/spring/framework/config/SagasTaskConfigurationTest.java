package co.orquex.sagas.spring.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SagasTaskConfiguration.class, MockConfiguration.class})
class SagasTaskConfigurationTest {

  @Autowired ApplicationContext applicationContext;

  @Test
  void shouldLoadTaskConfiguration() {
    assertThat(applicationContext.getBean("groovyActivity")).isNotNull();
    assertThat(applicationContext.getBean("groovyEvaluation")).isNotNull();
    assertThat(applicationContext.getBean("okHttpGetActivity")).isNotNull();
    assertThat(applicationContext.getBean("okHttpPostActivity")).isNotNull();
  }
}
