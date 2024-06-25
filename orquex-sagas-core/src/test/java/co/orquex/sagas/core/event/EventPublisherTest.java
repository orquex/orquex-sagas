package co.orquex.sagas.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.orquex.sagas.core.event.impl.DefaultEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.event.impl.SingleThreadEventLoop;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventPublisherTest {

  private static EventListenerFixture<String> listener;
  private static DefaultEventPublisher<String> eventPublisher;

  @BeforeAll
  static void beforeAll() {
    listener = new EventListenerFixture<>();
    final var eventSource = new InMemoryEventSource<String>();
    eventSource.addListener(listener);
    final var eventLoop = SingleThreadEventLoop.of(eventSource);
    eventLoop.start();
    eventPublisher = new DefaultEventPublisher<>(eventLoop);
  }

  @Test
  void testPublishEventMessage() {
    final var eventMessage = new EventMessage<>("message");
    eventPublisher.publish(eventMessage);
    await()
        .atMost(Duration.ofMillis(150))
        .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(eventMessage));
  }

  @Test
  void testPublishMessage() {
    final var message = "message";
    final var eventMessage = new EventMessage<>(message);
    eventPublisher.publish(message);
    await()
        .atMost(Duration.ofMillis(150))
        .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(eventMessage));
  }
}
