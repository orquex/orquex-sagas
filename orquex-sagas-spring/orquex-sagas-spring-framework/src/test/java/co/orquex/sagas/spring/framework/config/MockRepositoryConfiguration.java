package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injects the required bean repositories. */
@Configuration
public class MockRepositoryConfiguration {

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
  CompensationRepository compensationRepository() {
    return Mockito.mock(CompensationRepository.class);
  }
}
