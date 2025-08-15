package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.stage.DefaultStageExecutor;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnProperty;
import co.orquex.sagas.spring.framework.config.compensation.CompensationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the required Stage beans. */
@Slf4j
@Configuration
public class SagasStageConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"defaultStageExecutor"})
  public StageExecutor defaultStageExecutor(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository,
      CompensationHandler compensationHandler,
      RetryStateManager retryStateManager,
      CircuitBreakerStateManager circuitBreakerStateManager) {
    final var activityStrategy =
        new ActivityProcessingStrategy(
            taskExecutorRegistry,
            taskRepository,
            retryStateManager,
            circuitBreakerStateManager,
            compensationHandler);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(
            taskExecutorRegistry, taskRepository, retryStateManager, circuitBreakerStateManager);

    return new DefaultStageExecutor(activityStrategy, evaluationStrategy);
  }

  @Bean
  @ConditionalOnProperty(name = "orquex.sagas.spring.compensation.enabled", havingValue = "true")
  @ConditionalOnMissingBean(name = {"defaultCompensationHandler", "compensationHandler"})
  public CompensationHandler compensationHandler(CompensationRepository compensationRepository) {
    return compensation -> {
      log.debug(
          "Compensation received of flow '{}' with correlation '{}' and transaction '{}', task '{}'",
          compensation.flowId(),
          compensation.correlationId(),
          compensation.transactionId(),
          compensation.task());
      compensationRepository.save(compensation);
      log.debug(
          "Compensation saved of flow '{}' with correlation '{}' and transaction '{}', task '{}'",
          compensation.flowId(),
          compensation.correlationId(),
          compensation.transactionId(),
          compensation.task());
    };
  }

  @Bean
  @ConditionalOnProperty(
      name = "orquex.sagas.spring.compensation.enabled",
      havingValue = "false",
      matchIfMissing = true)
  @ConditionalOnMissingBean(name = {"defaultCompensationHandler", "compensationHandler"})
  public CompensationHandler defaultCompensationHandler() {
    return compensation ->
        log.debug(
            "Compensation received but not action taken for flow ID '{}', correlation ID '{}', transaction ID '{}' and task '{}'",
            compensation.flowId(),
            compensation.correlationId(),
            compensation.transactionId(),
            compensation.task());
  }
}
