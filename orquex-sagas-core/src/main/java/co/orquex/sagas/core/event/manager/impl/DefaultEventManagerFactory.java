package co.orquex.sagas.core.event.manager.impl;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;

import co.orquex.sagas.core.event.manager.EventManager;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * Default implementation of the {@link EventManagerFactory} interface. This class is responsible
 * for creating and managing instances of {@link EventManager}. It uses a ConcurrentHashMap to store
 * and retrieve EventManager instances for different event types.
 */
@Getter
public final class DefaultEventManagerFactory implements EventManagerFactory {

  /**
   * Map to store EventManager instances. The key is the class of the event type, and the value is
   * the EventManager instance for that event type.
   */
  private final Map<Class<?>, EventManager<?>> eventManagerMap;

  /** Default constructor that initializes the eventManagerMap. */
  public DefaultEventManagerFactory() {
    eventManagerMap = new ConcurrentHashMap<>();
  }

  /**
   * Gets or creates an event manager for a specific event type. If an event manager for the given
   * event type does not exist, it is created and stored.
   *
   * @param <T> the type of events the event manager handles
   * @param eventType the class of the event type
   * @return an event manager for the specified event type
   * @throws IllegalArgumentException if the eventType is null
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> EventManager<T> getEventManager(Class<T> eventType) {
    return (EventManager<T>)
        eventManagerMap.computeIfAbsent(
            checkArgumentNotNull(eventType, "Class event type required"),
            key -> new DefaultEventManager<>());
  }
}
