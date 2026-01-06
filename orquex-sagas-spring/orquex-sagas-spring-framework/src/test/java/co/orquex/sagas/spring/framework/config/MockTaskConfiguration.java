package co.orquex.sagas.spring.framework.config;

import static org.mockito.Mockito.when;

import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injects the required bean tasks configurations. */
@Configuration
public class MockTaskConfiguration {

  @Bean
  @SuppressWarnings("unchecked")
  HttpClientProvider<HttpClient> httpClientProvider() {
    final var mock = Mockito.mock(HttpClientProvider.class);
    when(mock.getKey()).thenReturn("mock-client-provider");
    return mock;
  }

  @Bean
  ObjectMapper objectMapper() {
    return Mockito.mock(ObjectMapper.class);
  }

  @Bean
  GlobalContext globalContext() {
    return Mockito.mock(GlobalContext.class);
  }
}
