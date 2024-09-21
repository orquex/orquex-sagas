package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.api.registry.Registrable;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import java.io.Serializable;
import java.util.Map;

/** Every implementation defines the communication with all tasks */
public interface TaskExecutor extends Registrable {

  Map<String, Serializable> execute(String transactionId, Task task, ExecutionRequest request);
}
