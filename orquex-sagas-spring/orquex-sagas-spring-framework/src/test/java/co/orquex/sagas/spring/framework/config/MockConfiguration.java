package co.orquex.sagas.spring.framework.config;

import static org.mockito.Mockito.when;

import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injects the required bean repositories. */
@Configuration
public class MockConfiguration {

  @Bean
  FlowRepository flowRepository() {
    return Mockito.mock(FlowRepository.class);
  }

  @Bean
  TransactionRepository transactionRepository() {
    return Mockito.mock(TransactionRepository.class);
  }

  @Bean
  TaskRepository taskRepository() {
    return Mockito.mock(TaskRepository.class);
  }

  @Bean
  @SuppressWarnings("unchecked")
  HttpClientProvider<OkHttpClient> okHttpClientProvider() {
    final var mock = Mockito.mock(HttpClientProvider.class);
    when(mock.getKey()).thenReturn("mock-client-provider");
    return mock;
  }

  @Bean
  ObjectMapper objectMapper() {
    return Mockito.mock(ObjectMapper.class);
  }
}
