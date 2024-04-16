package co.orquex.sagas.core.event;

import co.orquex.sagas.core.event.impl.EventMessage;

public interface EventLoop<T> {

  EventLoop<T> start();

  void push(EventMessage<T> message);

  boolean hasEvents();
}
