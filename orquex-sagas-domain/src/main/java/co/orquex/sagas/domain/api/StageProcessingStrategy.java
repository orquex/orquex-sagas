package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageResponse;

public interface StageProcessingStrategy<S extends Stage> {

    StageResponse process(String transactionId, S stage, ExecutionRequest request);
}
