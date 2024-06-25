package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the WorkflowEventPublisher interface. This class is responsible for
 * publishing events to the appropriate event manager.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultWorkflowEventPublisher implements WorkflowEventPublisher {

  /** Factory to create event managers. */
  private final EventManagerFactory eventManagerFactory;

  /**
   * Publishes an event message to the appropriate event manager.
   *
   * @param event the event message to publish
   * @param <T> the type of the event message
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> void publish(EventMessage<T> event) {
    log.trace("Publishing event {}", event);
    // Get the event message type
    final var messageClass = (Class<T>) event.message().getClass();
    // Get the specialized event manager
    eventManagerFactory.getEventManager(messageClass).getEventPublisher().publish(event);
  }
}
