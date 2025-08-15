package co.orquex.sagas.sample.service;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FailedTask implements TaskImplementation {

  @Override
  public Map<String, Serializable> execute(TaskRequest request) {
    throw new WorkflowException("Failed task executed");
  }

  @Override
  public String getKey() {
    return "failed";
  }
}
