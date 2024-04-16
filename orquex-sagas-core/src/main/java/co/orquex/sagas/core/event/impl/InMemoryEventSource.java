package co.orquex.sagas.core.event.impl;


import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.EventSource;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryEventSource<T> implements EventSource<T> {

  private final List<EventListener<T>> listeners = new ArrayList<>();

  @Override
  public void addListener(final EventListener<T> listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeListener(final EventListener<T> listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void broadcast(final EventMessage<T> message) {
    if (message.hasError()) this.listeners.forEach(listener -> listener.onError(message));
    this.listeners.forEach(listener -> listener.onMessage(message));
  }
}
