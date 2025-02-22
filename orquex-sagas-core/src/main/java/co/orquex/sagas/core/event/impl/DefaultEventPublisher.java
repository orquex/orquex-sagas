package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link EventPublisher} interface. This class is responsible for
 * publishing events to the event loop.
 *
 * @param <T> the type of the event message
 */
@Slf4j
public class DefaultEventPublisher<T> implements EventPublisher<T> {

  /** The event loop where events are published. */
  private final EventLoop<T> eventLoop;

  public DefaultEventPublisher(EventLoop<T> eventLoop) {
    log.debug("Creating a new DefaultEventPublisher instance with event loop {}", eventLoop.getClass().getSimpleName());
    this.eventLoop = eventLoop;
  }

  /**
   * Publishes an event message to the event loop.
   *
   * @param event the event message to publish
   */
  @Override
  public void publish(EventMessage<T> event) {
    this.eventLoop.push(event);
  }

  /**
   * Creates an event message from the given message and publishes it to the event loop.
   *
   * @param message the message to publish
   */
  @Override
  public void publish(T message) {
    final var eventMessage = new EventMessage<>(message);
    this.publish(eventMessage);
  }
}
