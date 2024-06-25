package co.orquex.sagas.core.event.manager;

/** Factory interface for obtaining event managers. */
public interface EventManagerFactory {

  /**
   * Gets or creates an event manager for a specific event type.
   *
   * @param <T> the type of events the event manager handles
   * @param eventType the class of the event type
   * @return an event manager for the specified event type
   */
  <T> EventManager<T> getEventManager(Class<T> eventType);
}
