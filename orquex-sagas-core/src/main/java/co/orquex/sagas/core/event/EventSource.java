package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * Exposes the methods to manage listeners and broadcast messages.
 *
 * @param <T> a message type
 */
public interface EventSource<T> {

  void addListener(EventListener<T> listener);

  void removeListener(EventListener<T> listener);

  void broadcast(EventMessage<T> message);
}
