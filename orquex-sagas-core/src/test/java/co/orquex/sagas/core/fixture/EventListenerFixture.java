package co.orquex.sagas.core.fixture;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class EventListenerFixture<T> implements EventListener<T> {

    private final List<EventMessage<T>> successMessages = new ArrayList<>();
    private final List<EventMessage<T>> errorMessages = new ArrayList<>();

    @Override
    public void onMessage(EventMessage<T> message) {
        successMessages.add(message);
    }

    @Override
    public void onError(EventMessage<T> message) {
        errorMessages.add(message);
    }
}
