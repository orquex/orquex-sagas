package co.orquex.sagas.core.stage.strategy;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;

public interface StageProcessingStrategy<S extends Stage> {

    StrategyResponse process(String transactionId, S stage, ExecutionRequest request);
}
