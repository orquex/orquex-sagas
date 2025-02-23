package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  ObjectMapper objectMapper() {
    return Mockito.mock(ObjectMapper.class);
  }

  @Bean
  CompensationRepository compensationRepository() {
    return Mockito.mock(CompensationRepository.class);
  }
}
