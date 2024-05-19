package co.orquex.sagas.sample.service;

import co.orquex.sagas.domain.api.TaskImplementation;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeneralErrorTask implements TaskImplementation {

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {
    final Map<String, Serializable> errors = new HashMap<>();
    errors.put("code", "0001");
    errors.put("message", "General error");

    return errors;
  }

  @Override
  public String getName() {
    return "general-error-task";
  }
}
