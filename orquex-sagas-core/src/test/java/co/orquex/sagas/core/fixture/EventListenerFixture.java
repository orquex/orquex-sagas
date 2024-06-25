package co.orquex.sagas.core.fixture;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;

@Getter
public class EventListenerFixture<T> implements EventListener<T> {

  private final ConcurrentLinkedQueue<EventMessage<T>> successMessages = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<EventMessage<T>> errorMessages = new ConcurrentLinkedQueue<>();

  @Override
  public void onMessage(EventMessage<T> message) {
    successMessages.add(message);
  }

  @Override
  public void onError(EventMessage<T> message) {
    errorMessages.add(message);
  }
}
