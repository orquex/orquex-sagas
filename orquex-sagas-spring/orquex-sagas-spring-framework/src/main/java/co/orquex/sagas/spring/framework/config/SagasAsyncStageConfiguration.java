package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.stage.DefaultAsyncStageExecutor;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the required Stage beans. */
@Configuration
public class SagasAsyncStageConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"defaultAsyncStageExecutor", "asyncStageExecutor"})
  public StageExecutor defaultAsyncStageExecutor(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository,
      WorkflowEventPublisher workflowEventPublisher) {
    // Decorate the strategies' implementations with an event handler
    final var activityStrategy =
        new ActivityProcessingStrategy(
            taskExecutorRegistry, taskRepository, workflowEventPublisher);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(taskExecutorRegistry, taskRepository);

    return new DefaultAsyncStageExecutor(
        activityStrategy, evaluationStrategy, workflowEventPublisher);
  }
}
