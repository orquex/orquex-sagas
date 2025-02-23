package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

/**
 * The EventLoop interface provides methods for managing events in the system. It allows starting
 * the event loop, pushing new events to it, and checking if there are any events left.
 *
 * @param <T> the type of the event message
 */
public interface EventLoop<T> {

  /**
   * Starts the event loop.
   *
   * @return the started event loop
   */
  EventLoop<T> start();

  /**
   * Pushes a new event message to the event loop.
   *
   * @param message the event message to be pushed
   */
  void push(EventMessage<T> message);

  /**
   * Checks if there are any events left in the event loop.
   *
   * @return true if there are events left, false otherwise
   */
  boolean hasEvents();

  /**
   * Checks if the event loop is alive.
   *
   * @return true if the event loop is alive, false otherwise
   */
  boolean isAlive();
}
