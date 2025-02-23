package co.orquex.sagas.core.fixture;

import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventManagerFactoryFixture {

  public static EventManagerFactory getEventManagerFactory() {
    return new DefaultEventManagerFactory();
  }
}
