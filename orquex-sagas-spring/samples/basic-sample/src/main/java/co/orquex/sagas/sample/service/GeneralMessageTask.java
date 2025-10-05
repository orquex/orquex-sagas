package co.orquex.sagas.sample.service;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GeneralMessageTask implements TaskImplementation {

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    final var metadata = taskRequest.metadata();
    final var code = metadata.getOrDefault("code", "0000");
    final var message = metadata.getOrDefault("message", "No message provided");

    final var errors = new HashMap<String, Serializable>();
    errors.put("code", code);
    errors.put("message", message);

    return errors;
  }

  @Override
  public String getKey() {
    return "general-message";
  }
}
