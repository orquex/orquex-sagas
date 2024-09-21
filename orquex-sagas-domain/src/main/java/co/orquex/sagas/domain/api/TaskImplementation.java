package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.api.registry.Registrable;
import java.io.Serializable;
import java.util.Map;

public interface TaskImplementation extends Registrable {

  Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload);
}
