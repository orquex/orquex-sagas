package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.EventLoop;
import co.orquex.sagas.core.event.EventSource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SingleThreadEventLoop<T> implements EventLoop<T> {

  private final BlockingQueue<EventMessage<T>> eventQueue;
  private final Thread eventLoopThread;

  private SingleThreadEventLoop(EventSource<T> eventSource) {
    this.eventQueue = new LinkedBlockingQueue<>();
    this.eventLoopThread = new Thread(new EventLoopExecutor<>(eventSource, this.eventQueue));
    Runtime.getRuntime().addShutdownHook(new Thread(eventLoopThread::interrupt));
  }

  public static <T> SingleThreadEventLoop<T> of(EventSource<T> eventSource) {
    return new SingleThreadEventLoop<>(eventSource);
  }

  @Override
  public EventLoop<T> start() {
    if (!this.eventLoopThread.isAlive()) eventLoopThread.start();
    return this;
  }

  @Override
  public void push(final EventMessage<T> message) {
    if (this.eventLoopThread.isAlive()) this.eventQueue.add(message);
  }

  @Override
  public boolean hasEvents() {
    return !eventQueue.isEmpty();
  }

  @Slf4j
  private record EventLoopExecutor<T>(
      EventSource<T> eventSource, BlockingQueue<EventMessage<T>> eventQueue) implements Runnable {

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
      log.debug(
          "Running single thread event loop from {}", this.eventSource.getClass().getSimpleName());
      try (final var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
        while (true) {
          var message = eventQueue.poll();
          if (message == null) continue;
          executorService.submit(() -> eventSource.broadcast(message));
        }
      }
    }
  }
}
