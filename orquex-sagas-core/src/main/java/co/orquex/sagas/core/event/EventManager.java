package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.event.impl.SingleThreadEventLoop;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class EventManager<T> {

  private final EventSource<T> eventSource;
  private final EventLoop<T> eventLoop;

  public EventManager() {
    this.eventSource = new InMemoryEventSource<>();
    this.eventLoop = SingleThreadEventLoop.of(eventSource).start();
    this.eventLoop.start();
  }

  public EventManager<T> addListener(final EventListener<T> listener) {
    this.eventSource.addListener(listener);
    log.debug("Added a new listener {}", listener.getClass().getSimpleName());
    return this;
  }

  public void removeListener(final EventListener<T> listener) {
    this.eventSource.removeListener(listener);
    log.debug("Listener {} removed", listener.getClass().getSimpleName());
  }

  public void send(EventMessage<T> message) {
    this.eventLoop.push(message);
  }

  public void send(T message) {
    final var eventMessage = EventMessage.<T>builder().message(message).build();
    this.send(eventMessage);
  }
}
