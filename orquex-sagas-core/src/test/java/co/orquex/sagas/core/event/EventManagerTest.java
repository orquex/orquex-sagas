package co.orquex.sagas.core.event;

import static org.awaitility.Awaitility.with;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManager;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EventManagerTest {

  @Test
  void shouldListenStringMessages() {
    final var listener = new EventListenerFixture<String>();
    final var eventManager = new DefaultEventManager<String>()
            .addListener(listener);
    final var message = new EventMessage<>("Hello world!");
    eventManager.getEventPublisher().publish(message);
    with()
        .pollInterval(Duration.ofMillis(1))
        .await()
        .atMost(Duration.ofMillis(2))
        .until(() -> listener.getSuccessMessages().size() == 1);
    eventManager.removeListener(listener);
  }
}
