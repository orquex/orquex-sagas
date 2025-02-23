package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * This interface defines the methods to publish events.
 *
 * @param <T> the type of the event message
 */
public interface EventPublisher<T> {

  /**
   * Publishes an event message.
   *
   * @param event the event message to publish
   */
  void publish(EventMessage<T> event);

  /**
   * Publishes a message.
   *
   * @param message the message to publish
   */
  void publish(T message);
}
