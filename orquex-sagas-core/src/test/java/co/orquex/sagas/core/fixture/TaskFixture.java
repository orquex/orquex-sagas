package co.orquex.sagas.core.fixture;

import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;

import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.task.TaskConfiguration;
import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.task.resilience.TaskCircuitBreakerConfiguration;
import co.orquex.sagas.domain.task.resilience.TaskResilienceConfiguration;
import co.orquex.sagas.domain.task.resilience.TaskRetryConfiguration;
import java.time.Duration;
import java.util.Map;

public final class TaskFixture {

  public static Task getTask(String id) {
    return new Task(id, id, id, null, null, null);
  }

  /** Creates a task with retry configuration. */
  public static Task getTaskWithRetryConfig(
      String taskId, long maxAttempts, Duration waitDuration) {
    final var retryConfig = new TaskRetryConfiguration(maxAttempts, waitDuration, true, null);
    final var resilienceConfig =
        new TaskResilienceConfiguration(Duration.ofMinutes(1), retryConfig, null);
    final var taskConfig =
        TaskConfiguration.builder().executor(DEFAULT_EXECUTOR).resilience(resilienceConfig).build();

    return new Task(taskId, taskId, taskId, null, null, taskConfig);
  }

  /** Creates a task with circuit breaker configuration. */
  public static Task getTaskWithCircuitBreakerConfig(
      String taskId,
      long failureThreshold,
      Duration waitDurationInOpenState,
      long successThreshold) {
    final var fallbackTask =
        new TaskProcessor("fallback-task-id", Map.of("result", "fallback-result"));
    final var cbConfig =
        new TaskCircuitBreakerConfiguration(
            failureThreshold, waitDurationInOpenState, successThreshold, null, fallbackTask);
    final var resilienceConfig =
        new TaskResilienceConfiguration(Duration.ofMinutes(1), null, cbConfig);
    final var taskConfig =
        TaskConfiguration.builder().executor(DEFAULT_EXECUTOR).resilience(resilienceConfig).build();

    return new Task(taskId, taskId, taskId, null, null, taskConfig);
  }

  /** Creates a task with both retry and circuit breaker configurations. */
  public static Task getTaskWithBothResilienceConfigs(
      String taskId,
      long maxAttempts,
      Duration waitDuration,
      long failureThreshold,
      Duration waitDurationInOpenState,
      long successThreshold) {
    final var fallbackTask =
        new TaskProcessor("fallback-task-id", Map.of("result", "fallback-result"));
    final var retryConfig = new TaskRetryConfiguration(maxAttempts, waitDuration, true, null);
    final var cbConfig =
        new TaskCircuitBreakerConfiguration(
            failureThreshold, waitDurationInOpenState, successThreshold, null, fallbackTask);
    final var resilienceConfig =
        new TaskResilienceConfiguration(Duration.ofMinutes(1), retryConfig, cbConfig);
    final var taskConfig =
        TaskConfiguration.builder().executor(DEFAULT_EXECUTOR).resilience(resilienceConfig).build();

    return new Task(taskId, taskId, taskId, null, null, taskConfig);
  }
}
