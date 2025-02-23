package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * Exposes the methods to manage listeners and broadcast event messages.
 *
 * @param <T> a message type
 */
public interface EventSource<T> {

  /**
   * Adds a listener to the event source.
   *
   * @param listener an event listener
   */
  void addListener(EventListener<T> listener);

  /**
   * Removes a listener from the event source.
   *
   * @param listener an event listener
   */
  void removeListener(EventListener<T> listener);

  /**
   * Broadcasts a message to all listeners.
   *
   * @param message an event message
   */
  void broadcast(EventMessage<T> message);
}
