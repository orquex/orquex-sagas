package co.orquex.sagas.core.stage.strategy;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

@Builder
public record StrategyResponse(Map<String, Serializable> payload, String outgoing) {

    public StrategyResponse {
        payload = checkArgumentNotNullOrElse(payload, new HashMap<>());
    }
}
