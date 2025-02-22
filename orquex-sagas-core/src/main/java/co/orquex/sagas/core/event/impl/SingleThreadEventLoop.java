package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventSource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * The SingleThreadEventLoop class provides a single-threaded event loop implementation. It uses a
 * blocking queue to store events and a single thread to process them.
 *
 * @param <T> the type of the event message
 */
@Slf4j
public final class SingleThreadEventLoop<T> implements EventLoop<T> {

  private final BlockingQueue<EventMessage<T>> eventQueue;
  private final Thread eventLoopThread;

  /**
   * Constructor that initializes the event queue and the event loop thread. It also adds a shutdown
   * hook to interrupt the event loop thread when the JVM is shutting down.
   *
   * @param eventSource the source of events
   */
  private SingleThreadEventLoop(EventSource<T> eventSource) {
    this.eventQueue = new LinkedBlockingQueue<>();
    this.eventLoopThread =
        new Thread(new EventLoopExecutor<>(eventSource, this.eventQueue), "event-loop");
    // Add a shutdown hook to interrupt the event loop thread when the JVM is shutting down
    Runtime.getRuntime().addShutdownHook(new Thread(eventLoopThread::interrupt));
  }

  /**
   * Factory method that creates a new SingleThreadEventLoop.
   *
   * @param eventSource the source of events
   * @return a new SingleThreadEventLoop
   */
  public static <T> SingleThreadEventLoop<T> of(EventSource<T> eventSource) {
    return new SingleThreadEventLoop<>(eventSource);
  }

  /**
   * Starts the event loop thread if it is not already running.
   *
   * @return this event loop
   */
  @Override
  public EventLoop<T> start() {
    // Start the event loop thread if it is not already running
    if (!this.eventLoopThread.isAlive()) eventLoopThread.start();
    return this;
  }

  /**
   * Adds a message to the event queue if the event loop thread is running.
   *
   * @param message the message to add
   */
  @Override
  public void push(final EventMessage<T> message) {
    // Add the message to the event queue if the event loop thread is running
    if (this.eventLoopThread.isAlive()) this.eventQueue.add(message);
  }

  /**
   * Checks if the event queue has any events.
   *
   * @return true if the event queue has any events, false otherwise
   */
  @Override
  public boolean hasEvents() {
    return !eventQueue.isEmpty();
  }

  /**
   * Checks if the event loop thread is alive.
   *
   * @return true if the event loop thread is alive, false otherwise
   */
  @Override
  public boolean isAlive() {
    return this.eventLoopThread.isAlive();
  }

  /**
   * The EventLoopExecutor class is responsible for running the event loop. It polls the event queue
   * for new messages and broadcasts them.
   *
   * @param <T> the type of the event message
   */
  @Slf4j
  private record EventLoopExecutor<T>(
      EventSource<T> eventSource, BlockingQueue<EventMessage<T>> eventQueue) implements Runnable {

    @Override
    public void run() {
      log.debug(
          "Running single thread event loop from {}", this.eventSource.getClass().getSimpleName());
      final var factory = Thread.ofVirtual().name("single-loop-", 0).factory();
      try (final var executorService = Executors.newThreadPerTaskExecutor(factory)) {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            // Take a message from the event queue or wait until one is available
            final var message = eventQueue.take();
            // Broadcast the message to all listeners once it is available
            executorService.submit(() -> eventSource.broadcast(message));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (Exception e) {
            log.error("Error while processing event", e);
          }
        }
      }
    }
  }
}
