package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.EventSource;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides an in-memory event source implementation. It stores a list of event listeners
 * and broadcasts messages to them.
 *
 * @param <T> the type of the event message
 */
@Slf4j
public final class InMemoryEventSource<T> implements EventSource<T> {

  /** The list of event listeners. */
  private final List<EventListener<T>> listeners = new ArrayList<>();

  @Override
  public void addListener(final EventListener<T> listener) {
    this.listeners.add(listener);
  }

  /**
   * Adds a listener to the list of event listeners.
   *
   * @param listener the listener to add
   */
  @Override
  public void removeListener(final EventListener<T> listener) {
    this.listeners.remove(listener);
  }

  /**
   * Broadcasts a message to all event listeners. If the message has an error, it calls the onError
   * method of the listener. Otherwise, it calls the onMessage method.
   *
   * @param message the message to broadcast
   */
  @Override
  public void broadcast(final EventMessage<T> message) {
    log.trace("Broadcasting message {} to {} listeners", message, listeners.size());
    if (message.hasError()) this.listeners.forEach(listener -> listener.onError(message));
    else this.listeners.forEach(listener -> listener.onMessage(message));
  }
}
