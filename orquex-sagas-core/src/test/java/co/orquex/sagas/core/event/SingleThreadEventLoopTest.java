package co.orquex.sagas.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.event.impl.SingleThreadEventLoop;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleThreadEventLoopTest {

  private EventLoop<String> eventLoop;
  private EventListenerFixture<String> listener;

  @BeforeEach
  void setUp() {
    listener = new EventListenerFixture<>();
    final var eventSource = new InMemoryEventSource<String>();
    eventSource.addListener(listener);
    eventLoop = SingleThreadEventLoop.of(eventSource);
    eventLoop.start();
  }

  @Test
  void testStart() {
    assertThat(eventLoop.isAlive()).isTrue();
  }

  @Test
  void testPushAndProcess() {
    final var message = new EventMessage<>("Test");
    eventLoop.push(message);
    await()
        .atMost(Duration.ofMillis(150))
        .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(message));
  }

  @Test
  void testHasEvents() {
    assertThat(eventLoop.hasEvents()).isFalse();
  }
}
