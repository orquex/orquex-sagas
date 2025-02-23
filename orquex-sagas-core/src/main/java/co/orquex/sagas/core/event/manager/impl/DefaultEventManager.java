package co.orquex.sagas.core.event.manager.impl;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventPublisher;
import co.orquex.sagas.core.event.EventSource;
import co.orquex.sagas.core.event.impl.DefaultEventPublisher;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.event.impl.SingleThreadEventLoop;
import co.orquex.sagas.core.event.manager.EventManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link EventManager} interface. This class is responsible for
 * managing events, including adding and removing listeners, and starting the event loop.
 *
 * @param <T> the type of events handled
 */
@Slf4j
@Getter
@AllArgsConstructor
public final class DefaultEventManager<T> implements EventManager<T> {

  private final EventSource<T> eventSource;
  private final EventLoop<T> eventLoop;
  private final EventPublisher<T> eventPublisher;

  /** Default constructor that initializes the event source, event loop, and event publisher. */
  public DefaultEventManager() {
    log.debug("Creating a new DefaultEventManager instance");
    this.eventSource = new InMemoryEventSource<>();
    this.eventLoop = SingleThreadEventLoop.of(eventSource).start();
    eventLoop.start();
    this.eventPublisher = new DefaultEventPublisher<>(eventLoop);
  }

  /**
   * Adds a listener to the event source and logs the addition.
   *
   * @param listener the listener to add
   * @return this event manager
   */
  public EventManager<T> addListener(final EventListener<T> listener) {
    this.eventSource.addListener(listener);
    log.debug("Added a new listener {}", listener.getClass().getSimpleName());
    return this;
  }

  /**
   * Removes a listener from the event source and logs the removal.
   *
   * @param listener the listener to remove
   */
  public void removeListener(final EventListener<T> listener) {
    this.eventSource.removeListener(listener);
    log.debug("Listener {} removed", listener.getClass().getSimpleName());
  }
}
