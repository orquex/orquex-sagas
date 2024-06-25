package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * Exposes the methods that will be implemented by interested classes will listen for and handle the
 * event.
 */
public interface EventListener<T> {

  /**
   * Method that will be called when the event is received.
   *
   * @param message the message that contains the event.
   */
  void onMessage(EventMessage<T> message);

  /**
   * Method that will be called when an error occurs while processing the event.
   *
   * @param message the message that contains the event.
   */
  void onError(EventMessage<T> message);
}
