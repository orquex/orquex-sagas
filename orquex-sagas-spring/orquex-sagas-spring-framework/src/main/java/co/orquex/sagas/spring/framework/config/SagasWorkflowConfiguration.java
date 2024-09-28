package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.core.flow.WorkflowStageExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import org.springframework.context.annotation.*;

/** Configure the required Workflow beans. */
@Configuration
public class SagasWorkflowConfiguration {

  @Bean
  public WorkflowExecutor workflowExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new WorkflowExecutor(workflowEventPublisher, flowRepository, transactionRepository);
  }

  @Bean
  public WorkflowStageExecutor workflowStageExecutor(
      final WorkflowEventPublisher workflowEventPublisher,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new WorkflowStageExecutor(workflowEventPublisher, flowRepository, transactionRepository);
  }
}
