package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * Exposes the methods that will be implemented by interested classes will listen for and handle the
 * event.
 */
public interface EventListener<T> {

  void onMessage(EventMessage<T> message);
  void onError(EventMessage<T> message);
}
