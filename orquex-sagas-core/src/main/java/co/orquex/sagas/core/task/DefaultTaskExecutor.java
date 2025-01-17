package co.orquex.sagas.core.task;

import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.Map;

import co.orquex.sagas.domain.utils.Maps;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultTaskExecutor implements TaskExecutor {

  private final Registry<TaskImplementation> taskRegistry;

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Task task, ExecutionRequest executionRequest) {

    final var implementation =
        taskRegistry
            .get(task.implementation())
            .orElseThrow(
                () ->
                    new WorkflowException(
                        "Task '%s' implementation not found".formatted(task.implementation())));
    // merge the task metadata with the current request metadata
    final var metadata = Maps.merge(executionRequest.metadata(), task.metadata());
    final var taskRequest =
        new TaskRequest(transactionId, metadata, executionRequest.payload());
    return implementation.execute(taskRequest);
  }

  @Override
  public String getKey() {
    return "default";
  }
}
