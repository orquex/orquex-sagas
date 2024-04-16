package co.orquex.sagas.core.task;

import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.task.Task;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultTaskExecutor implements TaskExecutor {

  private final Registry<TaskImplementation> taskRegistry;

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Task task, ExecutionRequest request) {

    final var implementation =
        taskRegistry
            .get(task.implementation())
            .orElseThrow(() -> new WorkflowException("task implementation not found"));
    return implementation.execute(transactionId, request.metadata(), request.payload());
  }

  @Override
  public String getId() {
    return "default";
  }
}
