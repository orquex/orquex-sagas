package co.orquex.sagas.core.event.manager;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.EventPublisher;

/**
 * Interface for managing events of a specific type.
 *
 * @param <T> the type of events handled
 */
public interface EventManager<T> {

  /**
   * Gets the event publisher associated with this event manager.
   *
   * @return the event publisher
   */
  EventPublisher<T> getEventPublisher();

  /**
   * Adds a listener to this event manager.
   *
   * @param listener the listener to add
   * @return the current event manager instance
   */
  EventManager<T> addListener(final EventListener<T> listener);

  /**
   * Removes a listener from this event manager.
   *
   * @param listener the listener to remove
   */
  void removeListener(final EventListener<T> listener);
}
