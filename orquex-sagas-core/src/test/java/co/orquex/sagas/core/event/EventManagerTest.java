package co.orquex.sagas.core.event;

import static org.awaitility.Awaitility.await;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EventManagerTest {

  @Test
  void shouldListenStringMessages() {
    var listener = new EventListenerFixture<String>();
    var eventManager = new EventManager<String>().addListener(listener);
    var message = new EventMessage<>("Hello world!");
    eventManager.send(message);
    await().atMost(Duration.ofMillis(150)).until(() -> listener.getSuccessMessages().size() == 1);
    eventManager.removeListener(listener);
  }
}
