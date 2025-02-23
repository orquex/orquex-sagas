package co.orquex.sagas.sample.service;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GeneralErrorTask implements TaskImplementation {

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    final Map<String, Serializable> errors = new HashMap<>();
    errors.put("code", "0001");
    errors.put("message", "General error");

    return errors;
  }

  @Override
  public String getKey() {
    return "general-error";
  }
}
