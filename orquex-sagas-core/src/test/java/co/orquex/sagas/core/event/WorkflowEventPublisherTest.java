package co.orquex.sagas.core.event;

import static org.awaitility.Awaitility.with;

import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.manager.EventManager;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import java.time.Duration;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowEventPublisherTest {

  private EventListenerFixture<Integer> integerEventListener;
  private DefaultWorkflowEventPublisher workflowEventPublisher;
  private EventManager<Integer> eventManager;

  @BeforeEach
  void setUp() {
    final var factory = new DefaultEventManagerFactory();
    eventManager = factory.getEventManager(Integer.class);
    integerEventListener = new EventListenerFixture<>();
    eventManager.addListener(integerEventListener);
    workflowEventPublisher = new DefaultWorkflowEventPublisher(factory);
  }

  @AfterEach
  void tearDown() {
    eventManager.removeListener(integerEventListener);
  }

  @Test
  void testPublishCheckpoint() {
    final int maxMessages = 10;
    IntStream.range(0, maxMessages)
        .forEach(i -> workflowEventPublisher.publish(new EventMessage<>(i)));
    with()
        .pollInterval(Duration.ofMillis(1))
        .await()
        .atMost(Duration.ofMillis(2))
        .until(() -> integerEventListener.getSuccessMessages().size() == maxMessages);
  }
}
