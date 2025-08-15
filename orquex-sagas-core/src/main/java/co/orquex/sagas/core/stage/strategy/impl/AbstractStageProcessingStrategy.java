package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.task.TaskExecutorService;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.util.Map;

/**
 * Abstract base class for stage processing strategies with built-in resilient task execution.
 *
 * <p>Provides common task execution functionality with automatic resilience patterns applied based
 * on task configuration. Concrete implementations define specific stage processing logic while
 * inheriting reliable task execution capabilities.
 *
 * <p>Task executions automatically apply retry and circuit breaker patterns when configured,
 * ensuring robust and fault-tolerant workflow execution.
 *
 * @param <S> the specific stage type this strategy processes
 * @since 1.0.0
 * @see StageProcessingStrategy
 * @see TaskExecutorService
 */
public abstract class AbstractStageProcessingStrategy<S extends Stage>
    implements StageProcessingStrategy<S> {

  protected TaskExecutorService taskExecutorService;

  /**
   * Creates a new strategy instance with the provided dependencies.
   *
   * <p>Initializes the task executor service with null state managers, meaning resilience patterns
   * will only be applied if configured at the task level.
   *
   * @param taskExecutorRegistry registry containing available task executors
   * @param taskRepository repository for retrieving task definitions
   */
  protected AbstractStageProcessingStrategy(
      final Registry<TaskExecutor> taskExecutorRegistry,
      final TaskRepository taskRepository,
      final RetryStateManager retryStateManager,
      final CircuitBreakerStateManager circuitBreakerStateManager) {
    this.taskExecutorService =
        TaskExecutorService.of(
            taskExecutorRegistry, taskRepository, retryStateManager, circuitBreakerStateManager);
  }

  /**
   * Executes a task with resilience patterns automatically applied based on task configuration.
   *
   * <p>Applies retry and circuit breaker patterns when configured. If no resilience configuration
   * is present, executes the task directly without additional overhead.
   *
   * @param transactionId unique identifier for the transaction context
   * @param taskId identifier of the task to execute
   * @param request execution request containing flow context, metadata, and payload
   * @return the execution result after applying configured resilience patterns
   * @throws WorkflowException if task or executor is not found
   * @throws WorkflowException if thrown by the task execution itself
   */
  protected Map<String, Serializable> executeTask(
      String transactionId, String taskId, ExecutionRequest request) {
    return taskExecutorService.executeResilientTask(transactionId, taskId, request);
  }

  /**
   * Executes a task processor with resilience patterns applied.
   *
   * <p>Merges the processor's metadata with the request metadata before execution.
   *
   * @param transactionId unique identifier for the transaction context
   * @param processor task processor containing task ID and additional metadata
   * @param request execution request to be merged with processor metadata
   * @return the execution result after applying configured resilience patterns
   * @throws WorkflowException if task or executor is not found
   * @throws WorkflowException if thrown by the task execution itself
   */
  protected Map<String, Serializable> executeProcessor(
      final String transactionId, final TaskProcessor processor, final ExecutionRequest request) {
    return this.executeTask(
        transactionId, processor.task(), request.mergeMetadata(processor.metadata()));
  }
}
