package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/** Exposes the method to publish any type of message. */
public interface WorkflowEventPublisher {

  /**
   * Publishes an event message to all listeners.
   *
   * @param event the event message to publish
   * @param <T> the type of the event message
   */
  <T> void publish(EventMessage<T> event);
}
