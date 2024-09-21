package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import co.orquex.sagas.task.okhttp.OkHttpClientProvider;
import co.orquex.sagas.task.okhttp.OkHttpGetActivity;
import co.orquex.sagas.task.okhttp.OkHttpInMemoryClientRegistry;
import co.orquex.sagas.task.okhttp.OkHttpPostActivity;
import java.util.List;
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

  @Bean
  public TaskImplementation okHttpGetActivity(Registry<OkHttpClientProvider> registry) {
    return new OkHttpGetActivity(registry);
  }

  @Bean
  public TaskImplementation okHttpPostActivity(Registry<OkHttpClientProvider> registry) {
    return new OkHttpPostActivity(registry);
  }

  @Bean
  public Registry<OkHttpClientProvider> okHttpInMemoryClientRegistry(
      List<OkHttpClientProvider> okHttpProviders) {
    return OkHttpInMemoryClientRegistry.of(okHttpProviders);
  }
}
