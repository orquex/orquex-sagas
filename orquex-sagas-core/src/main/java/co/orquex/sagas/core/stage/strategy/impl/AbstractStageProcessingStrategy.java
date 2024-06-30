package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.task.Task;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractStageProcessingStrategy<S extends Stage>
    implements StageProcessingStrategy<S> {

  protected final Registry<TaskExecutor> taskExecutorRegistry;
  protected final TaskRepository taskRepository;
  protected final WorkflowEventPublisher eventPublisher;

  protected Map<String, Serializable> executeTask(
      String transactionId, String taskId, ExecutionRequest request) {
    final var task = getTask(taskId);
    final var taskExecutor = getTaskExecutor(task);
    // merge the task metadata with the current metadata
    return taskExecutor.execute(transactionId, task, request.mergeMetadata(task.metadata()));
  }

  protected Map<String, Serializable> executeProcessor(
      final String transactionId, final TaskProcessor processor, final ExecutionRequest request) {
    return this.executeTask(
        transactionId, processor.task(), request.mergeMetadata(processor.metadata()));
  }

  protected Task getTask(String taskId) {
    return taskRepository
        .findById(taskId)
        .orElseThrow(() -> new WorkflowException("Task '%s' not found".formatted(taskId)));
  }

  protected TaskExecutor getTaskExecutor(Task task) {
    String executorId = task.configuration().executor();
    return taskExecutorRegistry
        .get(executorId)
        .orElseThrow(
            () -> new WorkflowException("Task executor '%s' not registered".formatted(executorId)));
  }
}
