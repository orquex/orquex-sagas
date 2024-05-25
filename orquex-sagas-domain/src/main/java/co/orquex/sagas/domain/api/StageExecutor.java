package co.orquex.sagas.domain.api;

import co.orquex.sagas.domain.stage.StageRequest;

/** The StageExecutor interface extends Executable for handling StageRequest objects. */
public interface StageExecutor extends Executable<StageRequest> {

  void execute(StageRequest stageRequest);

  /**
   * Returns an unique identifier of this stage executor implementation.
   *
   * @return a stage executor identifier.
   */
  String getId();
}
