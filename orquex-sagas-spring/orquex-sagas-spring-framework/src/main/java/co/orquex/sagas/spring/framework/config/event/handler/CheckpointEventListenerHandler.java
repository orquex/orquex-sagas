package co.orquex.sagas.spring.framework.config.event.handler;

import co.orquex.sagas.domain.transaction.Checkpoint;

/** Handle the checkpoint events and allows the continuation of the flow. */
public interface CheckpointEventListenerHandler {

  void handle(Checkpoint checkpoint);
}
