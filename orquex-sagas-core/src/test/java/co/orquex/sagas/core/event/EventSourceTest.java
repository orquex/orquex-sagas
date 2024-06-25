package co.orquex.sagas.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventSourceTest {

  private static EventListenerFixture<String> eventListener;

  @BeforeAll
  static void beforeAll() {
    eventListener = new EventListenerFixture<>();
  }

  @Test
  void testInMemoryEventSource() {
    final var message = new EventMessage<>("message");
    final var eventSource = new InMemoryEventSource<String>();
    eventSource.addListener(eventListener);
    eventSource.broadcast(message);
    await()
        .atMost(Duration.ofMillis(150))
        .untilAsserted(() -> assertThat(eventListener.getSuccessMessages()).contains(message));
  }
}
