package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.api.registry.Registrable;
import co.orquex.sagas.domain.stage.StageRequest;

/** The StageExecutor interface extends Executable for handling StageRequest objects. */
public interface StageExecutor extends Executable<StageRequest>, Registrable {

  void execute(StageRequest stageRequest);
}
