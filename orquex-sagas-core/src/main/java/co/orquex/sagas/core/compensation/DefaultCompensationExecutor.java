package co.orquex.sagas.core.compensation;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the CompensationExecutor interface. It's responsible for executing
 * compensations for a given task sequentially and synchronously.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCompensationExecutor implements CompensationExecutor {

  private final Registry<TaskExecutor> taskExecutorRegistry;
  private final TaskRepository taskRepository;
  private final CompensationRepository compensationRepository;

  /**
   * Executes the compensations for the given transaction ID.
   *
   * @param transactionId the ID of the transaction for which compensations need to be executed
   * @throws WorkflowException if a task executor or task is not found
   */
  @Override
  public void execute(String transactionId) {
    final var compensations = compensationRepository.findByTransactionId(transactionId);

    for (final var compensation : compensations) {
      try {
        var executionRequest =
            new ExecutionRequest(
                compensation.flowId(),
                compensation.correlationId(),
                compensation.metadata(),
                compensation.request());
        // Pre-process the payload with a task
        final var preProcessor = compensation.preProcessor();
        if (preProcessor != null) {
          log.debug(
              "Executing compensation pre-processor '{}' for task '{}'",
              preProcessor.task(),
              compensation.task());
          final var preProcessorPayload =
              executeProcessor(transactionId, preProcessor, executionRequest);
          executionRequest = executionRequest.withPayload(preProcessorPayload);
        }

        // Execute the task with the pre-processed payload if any or the original payload
        log.debug(
            "Executing compensation task '{}' at flow '{}' with correlation ID '{}'",
            compensation.task(),
            executionRequest.flowId(),
            executionRequest.correlationId());
        var compensationResponse =
            executeTask(transactionId, compensation.task(), executionRequest);

        // Post-process the response with a task if any and update the response
        final var postProcessor = compensation.postProcessor();
        if (postProcessor != null) {
          log.debug(
              "Executing compensation post-processor '{}' for task '{}'",
              postProcessor.task(),
              compensation.task());
          compensationResponse =
              executeProcessor(
                  transactionId, postProcessor, executionRequest.withPayload(compensationResponse));
        }
        log.trace(
            "Compensation response for transaction '{}' and task '{}': {}",
            transactionId,
            compensation.task(),
            compensationResponse);
        log.debug(
                "Compensation executed for transaction '{}' and task '{}'",
                transactionId,
                compensation.task());
      } catch (WorkflowException e) {
        log.error(
            "Compensation execution failed for transaction '{}' and task '{}'",
            transactionId,
            compensation.task());
      }
    }
  }

  protected Map<String, Serializable> executeTask(
      String transactionId, String taskId, ExecutionRequest request) {
    final var task = getTask(taskId);
    final var taskExecutor = getTaskExecutor(task);
    return taskExecutor.execute(transactionId, task, request);
  }

  protected Map<String, Serializable> executeProcessor(
      final String transactionId, final TaskProcessor processor, final ExecutionRequest request) {
    return this.executeTask(
        transactionId, processor.task(), request.mergeMetadata(processor.metadata()));
  }

  protected Task getTask(String taskId) {
    return taskRepository
        .findById(taskId)
        .orElseThrow(
            () ->
                new WorkflowException(
                    "Task '%s' not found when trying to perform compensation".formatted(taskId)));
  }

  protected TaskExecutor getTaskExecutor(Task task) {
    final var executorId = task.configuration().executor();
    return taskExecutorRegistry
        .get(executorId)
        .orElseThrow(
            () ->
                new WorkflowException(
                    "Task executor '%s' not found when trying to perform compensation"
                        .formatted(executorId)));
  }
}
