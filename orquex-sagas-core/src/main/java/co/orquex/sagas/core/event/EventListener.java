package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * Exposes the methods that are implemented by interested classes to listen for and handle the
 * event.
 */
public interface EventListener<T> {

  /**
   * Method that is called when the task response is received.
   *
   * @param message the message that contains the event.
   */
  default void onMessage(EventMessage<T> message) {
    throw new UnsupportedOperationException();
  }

  /**
   * Method that is called when an error occurs while processing the event.
   *
   * @param message the message that contains the event.
   */
  default void onError(EventMessage<T> message) {
    throw new UnsupportedOperationException();
  }
}
