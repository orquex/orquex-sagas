package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

@Builder
public record StageResponse(String transactionId, Map<String, Serializable> payload, String outgoing) implements Serializable {

  public StageResponse {
    payload = checkArgumentNotNullOrElse(payload, new HashMap<>());
  }
  
}
