package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import co.orquex.sagas.task.jsonata.JSONata4JActivity;
import co.orquex.sagas.task.okhttp.OkHttpDeleteActivity;
import co.orquex.sagas.task.okhttp.OkHttpGetActivity;
import co.orquex.sagas.task.okhttp.OkHttpInMemoryClientProviderRegistry;
import co.orquex.sagas.task.okhttp.OkHttpPostActivity;
import co.orquex.sagas.task.okhttp.OkHttpPutActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import okhttp3.OkHttpClient;
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
  public TaskImplementation okHttpGetActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    return new OkHttpGetActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation okHttpPostActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    return new OkHttpPostActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation okHttpDeleteActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    return new OkHttpDeleteActivity(registry, objectMapper);
  }

  @Bean
  public TaskImplementation okHttpPutActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    return new OkHttpPutActivity(registry, objectMapper);
  }

  @Bean
  public HttpClientProviderRegistry<OkHttpClient> okHttpInMemoryClientRegistry(
      List<HttpClientProvider<OkHttpClient>> okHttpClientProviders) {
    return OkHttpInMemoryClientProviderRegistry.of(okHttpClientProviders);
  }

  @Bean
  public TaskImplementation jSONata4JActivity(
      GlobalContext globalContext, ObjectMapper objectMapper) {
    return new JSONata4JActivity(globalContext, objectMapper);
  }
}
