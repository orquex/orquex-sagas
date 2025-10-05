package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventSource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * The SingleThreadEventLoop class provides a single-threaded event loop implementation. It uses a
 * bounded blocking queue to store events and a single thread to process them sequentially.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Bounded queue with configurable capacity (default: 10,000 messages)
 *   <li>Thread-safe start/stop operations
 *   <li>Graceful shutdown that processes remaining events
 *   <li>Configurable thread naming for debugging
 *   <li>Automatic JVM shutdown hook registration
 * </ul>
 *
 * <p>Event processing is delegated to the {@link EventSource}, which is responsible for
 * broadcasting messages to listeners. Any errors during listener execution should be handled by the
 * listeners themselves, as the event loop only ensures message delivery.
 *
 * <p>Thread safety: This class is thread-safe. Multiple threads can safely call {@link #push},
 * {@link #start}, and {@link #stop} concurrently.
 *
 * @param <T> the type of the event message
 */
@Slf4j
public final class SingleThreadEventLoop<T> implements EventLoop<T> {

  private static final int DEFAULT_QUEUE_CAPACITY = 10000;
  public static final String DEFAULT_EVENT_LOOP_THREAD_NAME = "event-loop";

  private final BlockingQueue<EventMessage<T>> eventQueue;
  private final Thread eventLoopThread;
  private final Thread shutdownHook;
  private volatile boolean running = false;

  /**
   * Constructor that initializes the event queue and the event loop thread. It also adds a shutdown
   * hook to interrupt the event loop thread when the JVM is shutting down.
   *
   * <p>The event queue is bounded to prevent memory exhaustion. If the queue reaches capacity,
   * subsequent {@link #push} calls will fail and log an error.
   *
   * @param eventSource the source of events to broadcast messages to
   * @param queueCapacity the maximum capacity of the event queue (must be positive)
   * @param eventLoopThreadName the name of the event loop thread for debugging purposes
   */
  private SingleThreadEventLoop(
      EventSource<T> eventSource, int queueCapacity, String eventLoopThreadName) {
    this.eventQueue = new LinkedBlockingQueue<>(queueCapacity);
    this.eventLoopThread =
        new Thread(new EventLoopExecutor<>(eventSource, this.eventQueue), eventLoopThreadName);
    // Add a shutdown hook to interrupt the event loop thread when the JVM is shutting down
    this.shutdownHook = new Thread(eventLoopThread::interrupt);
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /**
   * Factory method that creates a new SingleThreadEventLoop with default configuration.
   *
   * <p>Uses default queue capacity of {@value #DEFAULT_QUEUE_CAPACITY} and thread name "{@value
   * #DEFAULT_EVENT_LOOP_THREAD_NAME}".
   *
   * @param <T> the type of the event message
   * @param eventSource the source of events to broadcast messages to
   * @return a new SingleThreadEventLoop instance
   */
  public static <T> SingleThreadEventLoop<T> of(EventSource<T> eventSource) {
    return new SingleThreadEventLoop<>(
        eventSource, DEFAULT_QUEUE_CAPACITY, DEFAULT_EVENT_LOOP_THREAD_NAME);
  }

  /**
   * Factory method that creates a new SingleThreadEventLoop with custom configuration.
   *
   * <p>Allows customization of queue capacity and thread name for fine-grained control over
   * resource usage and debugging.
   *
   * @param <T> the type of the event message
   * @param eventSource the source of events to broadcast messages to
   * @param queueCapacity the maximum capacity of the event queue (must be positive)
   * @param eventLoopThreadName the name of the event loop thread for debugging purposes
   * @return a new SingleThreadEventLoop instance
   */
  public static <T> SingleThreadEventLoop<T> of(
      EventSource<T> eventSource, int queueCapacity, String eventLoopThreadName) {
    return new SingleThreadEventLoop<>(eventSource, queueCapacity, eventLoopThreadName);
  }

  /**
   * Starts the event loop thread if it is not already running.
   *
   * <p>This method is thread-safe and idempotent. Multiple calls to start will have no effect if
   * the event loop is already running. Once started, the event loop will process messages from the
   * queue until {@link #stop()} is called or the JVM shuts down.
   *
   * @return this event loop instance for method chaining
   */
  @Override
  public synchronized EventLoop<T> start() {
    // Start the event loop thread if it is not already running
    if (!this.eventLoopThread.isAlive() && !running) {
      running = true;
      eventLoopThread.start();
    }
    return this;
  }

  /**
   * Adds a message to the event queue if the event loop is running.
   *
   * <p>This method attempts to add the message to the bounded queue. If the queue is at capacity,
   * the message will be rejected and an error will be logged. If the event loop is not running, a
   * warning will be logged and the message will be ignored.
   *
   * <p>This method is thread-safe and non-blocking. It returns immediately after attempting to add
   * the message to the queue.
   *
   * @param message the message to add to the event queue
   */
  @Override
  public void push(final EventMessage<T> message) {
    // Add the message to the event queue if the event loop is running
    if (!running) {
      log.warn("Attempting to push event to stopped event loop, event will be ignored");
      return;
    }
    if (!this.eventQueue.offer(message)) {
      log.error("Failed to add event to queue, queue may be full");
    }
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
   * Stops the event loop gracefully with a specified timeout.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Sets the running flag to false, preventing new messages from being added
   *   <li>Removes the JVM shutdown hook to prevent memory leaks
   *   <li>Interrupts the event loop thread to stop blocking on queue.take()
   *   <li>Waits for the thread to finish processing remaining events in the queue
   *   <li>Returns true if shutdown completed within timeout, false otherwise
   * </ol>
   *
   * <p>This method is thread-safe and idempotent. Multiple calls will have no effect if the event
   * loop is already stopped.
   *
   * @param timeoutMillis maximum time to wait for shutdown in milliseconds (must be positive)
   * @return true if the event loop stopped cleanly within the timeout, false otherwise
   */
  public synchronized boolean stop(long timeoutMillis) {
    if (!running) {
      return true;
    }
    running = false;

    // Remove the shutdown hook to prevent memory leak
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
      // JVM is already shutting down, ignore
      log.debug("Cannot remove shutdown hook, JVM is shutting down");
    }

    // Interrupt the event loop thread
    eventLoopThread.interrupt();

    // Wait for the thread to finish
    try {
      eventLoopThread.join(timeoutMillis);
      return !eventLoopThread.isAlive();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Stops the event loop gracefully with a default timeout of 5 seconds.
   *
   * <p>This is a convenience method that calls {@link #stop(long)} with a timeout of 5000
   * milliseconds. See {@link #stop(long)} for detailed behavior.
   *
   * @return true if the event loop stopped cleanly within 5 seconds, false otherwise
   */
  public boolean stop() {
    return stop(5000);
  }

  /**
   * The EventLoopExecutor class is responsible for running the event loop. It continuously polls
   * the event queue for new messages and submits them for broadcasting via virtual threads.
   *
   * <p>Key behaviors:
   *
   * <ul>
   *   <li>Blocks on {@code eventQueue.take()} waiting for messages
   *   <li>Submits each message to a virtual thread pool for asynchronous broadcasting
   *   <li>Handles interruption gracefully by processing remaining queued events
   *   <li>Catches unexpected exceptions to prevent loop termination
   * </ul>
   *
   * <p>Error handling: Errors during listener execution should be handled by the listeners
   * themselves. This executor only catches infrastructure-level exceptions (e.g., queue operations)
   * to ensure the event loop remains resilient.
   *
   * @param <T> the type of the event message
   */
  @Slf4j
  private record EventLoopExecutor<T>(
      EventSource<T> eventSource, BlockingQueue<EventMessage<T>> eventQueue) implements Runnable {

    @Override
    public void run() {
      final var eventSourceName = this.eventSource.getClass().getSimpleName();
      log.debug("Running single thread event loop from {}", eventSourceName);
      final var factory = Thread.ofVirtual().name("single-loop-", 0).factory();
      try (final var executorService = Executors.newThreadPerTaskExecutor(factory)) {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            log.trace("Running event loop iteration for {}", eventSourceName);
            // Take a message from the event queue or wait until one is available
            final var message = eventQueue.take();
            // Broadcast the message to all listeners once it is available
            executorService.submit(() -> eventSource.broadcast(message));
          } catch (InterruptedException e) {
            // Interrupt received, exit loop to process remaining events
            Thread.currentThread().interrupt();
          } catch (Exception e) {
            log.error("Error while processing event", e);
          }
        }

        // Process remaining events in the queue before shutting down
        log.debug("Processing remaining {} events before shutdown", eventQueue.size());
        EventMessage<T> remainingMessage;
        while ((remainingMessage = eventQueue.poll()) != null) {
          final var message = remainingMessage;
          executorService.submit(() -> eventSource.broadcast(message));
        }
      }
      log.debug("Single thread event loop stopped from {}", eventSourceName);
    }
  }
}
