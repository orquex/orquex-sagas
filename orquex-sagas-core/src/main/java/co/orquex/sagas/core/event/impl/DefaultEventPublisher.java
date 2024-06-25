package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventPublisher;
import lombok.RequiredArgsConstructor;

/**
 * Default implementation of the {@link EventPublisher} interface. This class is responsible for
 * publishing events to the event loop.
 *
 * @param <T> the type of the event message
 */
@RequiredArgsConstructor
public class DefaultEventPublisher<T> implements EventPublisher<T> {

  /** The event loop where events are published. */
  private final EventLoop<T> eventLoop;

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
