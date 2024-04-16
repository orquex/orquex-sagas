package co.orquex.sagas.domain.api;

import java.io.Serializable;
import java.util.Map;

public interface TaskImplementation {

  Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload);

  String getName();
}
