package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.api.registry.Registrable;
import co.orquex.sagas.domain.task.TaskRequest;

import java.io.Serializable;
import java.util.Map;

public interface TaskImplementation extends Registrable {

  Map<String, Serializable> execute(TaskRequest request);
}
