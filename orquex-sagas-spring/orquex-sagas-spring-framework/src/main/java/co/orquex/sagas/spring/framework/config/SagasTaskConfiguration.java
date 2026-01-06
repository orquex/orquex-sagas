package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import co.orquex.sagas.task.http.client.*;
import co.orquex.sagas.task.jsonata.JSONata4JActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure default tasks implementations. */
@Configuration
public class SagasTaskConfiguration {

  @Bean
  public TaskImplementation groovyActivity(GlobalContext globalContext) {
    return new GroovyActivity(globalContext);
  }

  @Bean
  public TaskImplementation groovyEvaluation(GlobalContext globalContext) {
    return new GroovyEvaluation(globalContext);
  }

  @Bean
  public TaskImplementation httpClientGetActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    return new HttpClientGetActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation httpClientPostActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    return new HttpClientPostActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation httpClientDeleteActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    return new HttpClientDeleteActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation httpClientPutActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    return new HttpClientGetActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation httpClientPatchActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    return new HttpClientPatchActivity(registry, objectMapper);
  }

  @Bean
  public HttpClientProviderRegistry<HttpClient> httpClientInMemoryClientRegistry(
      List<HttpClientProvider<HttpClient>> httpClientProviders) {
    return HttpClientInMemoryClientProviderRegistry.of(httpClientProviders);
  }

  @Bean
  public TaskImplementation jSONata4JActivity(
      GlobalContext globalContext, ObjectMapper objectMapper) {
    return new JSONata4JActivity(globalContext, objectMapper);
  }
}
