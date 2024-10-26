package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.flow.AsyncWorkflowExecutor;
import co.orquex.sagas.core.flow.AsyncWorkflowStageExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

/** Configure the required {@link AsyncWorkflowExecutor} beans. */
@Configuration
public class SagasAsyncWorkflowConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"asyncWorkflowExecutor"})
  public AsyncWorkflowExecutor asyncWorkflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new AsyncWorkflowExecutor(workflowEventPublisher, flowRepository, transactionRepository);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"asyncWorkflowStageExecutor"})
  public AsyncWorkflowStageExecutor asyncWorkflowStageExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new AsyncWorkflowStageExecutor(
        workflowEventPublisher, flowRepository, transactionRepository);
  }
}
