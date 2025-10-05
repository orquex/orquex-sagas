package co.orquex.sagas.core.task;

import co.orquex.sagas.core.resilience.*;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.task.resilience.TaskCircuitBreakerConfiguration;
import co.orquex.sagas.domain.task.resilience.TaskResilienceConfiguration;
import co.orquex.sagas.domain.task.resilience.TaskRetryConfiguration;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service for executing tasks with optional resilience patterns (retry and circuit breaker).
 *
 * <p>Provides two execution modes:
 *
 * <ul>
 *   <li><strong>Direct:</strong> Simple task execution without resilience
 *   <li><strong>Resilient:</strong> Automatic retry and circuit breaker based on task configuration
 * </ul>
 *
 * <p>Resilient execution automatically applies retry and circuit breaker patterns when configured,
 * ensuring robust and fault-tolerant task execution.
 *
 * @since 1.0.0
 * @see TaskExecutor
 * @see Task
 * @see TaskResilienceConfiguration
 * @see Retry
 * @see CircuitBreaker
 */
public record TaskExecutorService(
    Registry<TaskExecutor> taskExecutorRegistry,
    TaskRepository taskRepository,
    RetryStateManager retryStateManager,
    CircuitBreakerStateManager circuitBreakerStateManager) {

  /**
   * Factory method for creating a TaskExecutorService instance.
   *
   * <p>This is the preferred way to create service instances as it encapsulates the construction
   * logic and provides a clean API.
   *
   * @param taskExecutorRegistry registry containing available task executors. Must not be null.
   * @param taskRepository repository for retrieving task definitions. Must not be null.
   * @param retryStateManager manager for retry state persistence. Can be null if retry is not
   *     needed.
   * @param circuitBreakerStateManager manager for circuit breaker state. Can be null if circuit
   *     breaker is not needed.
   * @return a new TaskExecutorService instance configured with the provided dependencies
   * @throws NullPointerException if taskExecutorRegistry or taskRepository is null
   */
  public static TaskExecutorService of(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository,
      RetryStateManager retryStateManager,
      CircuitBreakerStateManager circuitBreakerStateManager) {
    return new TaskExecutorService(
        taskExecutorRegistry, taskRepository, retryStateManager, circuitBreakerStateManager);
  }

  /**
   * Executes a task directly without applying any resilience patterns.
   *
   * <p>This method provides simple, straightforward task execution without retry or circuit breaker
   * protection. It's suitable for scenarios where resilience is not required or is handled
   * externally.
   *
   * <p>The execution flow:
   *
   * <ol>
   *   <li>Retrieves the task definition from the repository
   *   <li>Resolves the appropriate task executor
   *   <li>Delegates execution to the resolved executor
   * </ol>
   *
   * @param transactionId unique identifier for the transaction context. Used for correlation and
   *     tracing.
   * @param taskId identifier of the task to execute. Must correspond to a task in the repository.
   * @param request execution request containing flow context, metadata, and payload
   * @return the execution result as a map of serializable values returned by the task executor
   * @throws WorkflowException if the task is not found in the repository
   * @throws WorkflowException if no executor is registered for the task's executor type
   * @throws WorkflowException if thrown by the task execution itself
   * @throws RuntimeException if task execution fails (propagated from the underlying executor)
   * @see #executeResilientTask(String, String, ExecutionRequest) for resilient execution
   */
  public Map<String, Serializable> executeTask(
      String transactionId, String taskId, ExecutionRequest request) {
    final var task = getTask(taskId);
    return executeTask(transactionId, task, request);
  }

  /**
   * Executes a task directly using the provided task instance without resilience patterns.
   *
   * <p>This overload accepts a pre-retrieved task instance, avoiding the repository lookup. It's
   * useful when the task has already been fetched or when working with dynamically created tasks.
   *
   * @param transactionId unique identifier for the transaction context
   * @param task the task instance to execute. Must not be null and must have a valid configuration.
   * @param request execution request containing flow context, metadata, and payload
   * @return the execution result as a map of serializable values
   * @throws WorkflowException if no executor is registered for the task's executor type
   * @throws WorkflowException if thrown by the task execution itself
   * @throws RuntimeException if task execution fails (propagated from the underlying executor)
   */
  public Map<String, Serializable> executeTask(
      String transactionId, Task task, ExecutionRequest request) {
    final var taskExecutor = getTaskExecutor(task);
    return taskExecutor.execute(transactionId, task, request);
  }

  /**
   * Executes a task with automatic resilience patterns applied based on the task's configuration.
   *
   * <p>This is the primary method for resilient task execution. It automatically examines the
   * task's resilience configuration and applies the appropriate patterns in the correct order:
   *
   * <ol>
   *   <li><strong>Base Execution:</strong> The core task execution logic
   *   <li><strong>Retry Wrapper:</strong> Applied if {@link TaskRetryConfiguration} is present
   *   <li><strong>Circuit Breaker Wrapper:</strong> Applied if {@link
   *       TaskCircuitBreakerConfiguration} is present
   * </ol>
   *
   * <p>Retry handles transient failures with configurable attempts and delays, while circuit
   * breaker provides system protection against cascading failures. If no configuration is present,
   * the task executes directly without additional overhead.
   *
   * <h4>Resilience Configuration:</h4>
   *
   * <p>Resilience behavior is determined by the task's {@link TaskResilienceConfiguration}:
   *
   * <ul>
   *   <li>If neither retry nor circuit breaker is configured, executes directly
   *   <li>If only retry is configured, applies retry only
   *   <li>If only circuit breaker is configured, applies circuit breaker only
   *   <li>If both are configured, applies retry first, then circuit breaker
   * </ul>
   *
   * @param transactionId unique identifier for the transaction context. Used for correlation and
   *     state management.
   * @param taskId identifier of the task to execute. Must correspond to a task in the repository.
   * @param request execution request containing flow context, metadata, and payload
   * @return the execution result after applying configured resilience patterns
   * @throws WorkflowException if the task is not found in the repository
   * @throws WorkflowException if no executor is registered for the task's executor type
   * @throws WorkflowException if thrown by the task execution itself
   * @throws RuntimeException if task execution fails after exhausting retry attempts (if
   *     configured)
   * @throws co.orquex.sagas.core.resilience.exception.CircuitBreakerOpenException if circuit
   *     breaker is open and no fallback is configured
   * @see TaskResilienceConfiguration for configuration options
   * @see TaskRetryConfiguration for retry behavior configuration
   * @see TaskCircuitBreakerConfiguration for circuit breaker behavior configuration
   * @see #executeTask(String, String, ExecutionRequest) for direct execution without resilience
   */
  public Map<String, Serializable> executeResilientTask(
      String transactionId, String taskId, ExecutionRequest request) {

    final var task = getTask(taskId);
    final var resilienceConfig = task.configuration().resilience();

    // Create TaskExecutionContext for resilience configuration
    final var context =
        new TaskExecutionContext(transactionId, request.flowId(), request.correlationId(), this);

    // Base task execution supplier
    final Supplier<Map<String, Serializable>> baseExecution =
        () -> executeTask(transactionId, task, request);

    // Apply retry wrapper if configuration exists: r(X)
    final var retryWrapped =
        applyRetryIfConfigured(taskId, context, resilienceConfig, baseExecution);

    // Apply circuit breaker wrapper around retry: cb(r(X))
    final var circuitBreakerWrapped =
        applyCircuitBreakerIfConfigured(taskId, request, context, resilienceConfig, retryWrapped);

    // Execute the composed function
    return circuitBreakerWrapped.get();
  }

  /** Applies retry configuration if present, otherwise returns identity function. */
  private Supplier<Map<String, Serializable>> applyRetryIfConfigured(
      String taskId,
      TaskExecutionContext context,
      TaskResilienceConfiguration resilienceConfig,
      Supplier<Map<String, Serializable>> taskExecution) {

    final Optional<TaskRetryConfiguration> retryConfig =
        Optional.ofNullable(resilienceConfig).map(TaskResilienceConfiguration::retry);

    if (retryConfig.isEmpty()) {
      // Identity function - no retry configuration
      return taskExecution;
    }

    final var retryConfiguration =
        new RetryConfiguration(
            taskId,
            retryConfig.get().maxAttempts(),
            retryConfig.get().waitDuration(),
            retryConfig.get().retryWorkflowException(),
            retryConfig.get().successPolicyTask(),
            retryStateManager,
            context);

    final var retry = Retry.of(retryConfiguration);
    return () -> retry.call(taskExecution);
  }

  /** Applies circuit breaker configuration if present, otherwise returns identity function. */
  private Supplier<Map<String, Serializable>> applyCircuitBreakerIfConfigured(
      String taskId,
      ExecutionRequest request,
      TaskExecutionContext context,
      TaskResilienceConfiguration resilienceConfig,
      Supplier<Map<String, Serializable>> retryWrappedExecution) {

    final Optional<TaskCircuitBreakerConfiguration> optionalTaskCircuitBreakerConfiguration =
        Optional.ofNullable(resilienceConfig).map(TaskResilienceConfiguration::circuitBreaker);

    if (optionalTaskCircuitBreakerConfiguration.isEmpty()) {
      // Identity function - no circuit breaker taskCircuitBreakerConfiguration
      return retryWrappedExecution;
    }

    final var taskCircuitBreakerConfiguration = optionalTaskCircuitBreakerConfiguration.get();
    final var fallback =
        new CircuitBreakerFallback(
            request.metadata(), request.payload(), taskCircuitBreakerConfiguration.fallbackTask());

    final var circuitBreakerConfiguration =
        new CircuitBreakerConfiguration(
            taskId,
            taskCircuitBreakerConfiguration.failureThreshold(),
            taskCircuitBreakerConfiguration.waitDurationInOpenState(),
            taskCircuitBreakerConfiguration.successThreshold(),
            taskCircuitBreakerConfiguration.successPolicyTask(),
            context,
            fallback,
            circuitBreakerStateManager);

    final var circuitBreaker = CircuitBreaker.of(circuitBreakerConfiguration);
    return () -> circuitBreaker.call(retryWrappedExecution);
  }

  private Task getTask(String taskId) {
    return taskRepository
        .findById(taskId)
        .orElseThrow(() -> new WorkflowException("Task '%s' not found".formatted(taskId)));
  }

  private TaskExecutor getTaskExecutor(Task task) {
    String executorId = task.configuration().executor();
    return taskExecutorRegistry
        .get(executorId)
        .orElseThrow(
            () -> new WorkflowException("Task executor '%s' not registered".formatted(executorId)));
  }
}
