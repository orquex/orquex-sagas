package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure default tasks implementations. */
@Configuration
public class SagasTaskConfiguration {

  @Bean
  public TaskImplementation groovyActivity() {
    return new GroovyActivity();
  }

  @Bean
  public TaskImplementation groovyEvaluation() {
    return new GroovyEvaluation();
  }
}
