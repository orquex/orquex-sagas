package co.orquex.sagas.sample.cs.api.event;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.Getter;

@Getter
public class CheckpointEventMessage {

    private final Checkpoint message;
    private final Error error;

    public CheckpointEventMessage(EventMessage<Checkpoint> eventMessage) {
        this.message = eventMessage.getMessage();
        this.error = eventMessage.getError();
    }
}
