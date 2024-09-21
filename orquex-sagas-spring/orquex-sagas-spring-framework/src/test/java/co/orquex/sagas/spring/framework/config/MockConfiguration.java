package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.task.okhttp.OkHttpClientProvider;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.when;

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
  OkHttpClientProvider okHttpClientProvider() {
    final var mock =  Mockito.mock(OkHttpClientProvider.class);
    when(mock.getKey()).thenReturn("mock-client-provider");
    return mock;
  }
}
