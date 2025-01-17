package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.compensation.DefaultCompensationExecutor;
import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

/** Configure the required {@link WorkflowExecutor} beans. */
@Configuration
public class SagasWorkflowConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"workflowExecutor"})
  public WorkflowExecutor workflowExecutor(
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository,
      @Qualifier("defaultStageExecutor") final StageExecutor defaultStageExecutor,
      final CompensationExecutor compensationExecutor,
      final ExecutorService workflowExecutorService) {
    return new WorkflowExecutor(
        flowRepository,
        transactionRepository,
        defaultStageExecutor,
        compensationExecutor,
        workflowExecutorService);
  }

  @Bean
  public CompensationExecutor defaultCompensationExecutor(
      final Registry<TaskExecutor> taskExecutorRegistry,
      final TaskRepository taskRepository,
      final CompensationRepository compensationRepository) {
    return new DefaultCompensationExecutor(
        taskExecutorRegistry, taskRepository, compensationRepository);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"workflowExecutorService"})
  public ExecutorService workflowExecutorService() {
    final var threadFactory = Thread.ofPlatform().name("workflow-executor-", 0).factory();
    return Executors.newFixedThreadPool(10, threadFactory);
  }
}
