package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.api.registry.Registrable;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;

/** The StageExecutor interface extends Executable for handling StageRequest objects. */
public interface StageExecutor extends Registrable {

  StageResponse execute(StageRequest stageRequest);
}
