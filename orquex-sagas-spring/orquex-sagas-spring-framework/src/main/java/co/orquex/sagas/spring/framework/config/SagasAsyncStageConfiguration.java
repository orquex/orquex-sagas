package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.stage.DefaultAsyncStageExecutor;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnProperty;
import co.orquex.sagas.spring.framework.config.compensation.AsyncCompensationHandler;
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
      WorkflowEventPublisher workflowEventPublisher,
      AsyncCompensationHandler asyncCompensationHandler,
      RetryStateManager retryStateManager,
      CircuitBreakerStateManager circuitBreakerStateManager) {
    // Decorate the strategies' implementations with an event handler
    final var activityStrategy =
        new ActivityProcessingStrategy(
            taskExecutorRegistry,
            taskRepository,
            retryStateManager,
            circuitBreakerStateManager,
            asyncCompensationHandler);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(
            taskExecutorRegistry, taskRepository, retryStateManager, circuitBreakerStateManager);

    return new DefaultAsyncStageExecutor(
        activityStrategy, evaluationStrategy, workflowEventPublisher);
  }

  @Bean({"defaultAsyncCompensationHandler", "asyncCompensationHandler"})
  @ConditionalOnProperty(
      name = "orquex.sagas.spring.compensation.enabled",
      havingValue = "true",
      matchIfMissing = true)
  @ConditionalOnMissingBean(name = {"defaultAsyncCompensationHandler", "asyncCompensationHandler"})
  public AsyncCompensationHandler asyncCompensationHandler(
      WorkflowEventPublisher workflowEventPublisher) {
    return compensation -> workflowEventPublisher.publish(new EventMessage<>(compensation));
  }
}
