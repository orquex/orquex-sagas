package co.orquex.sagas.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.domain.transaction.Checkpoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventManagerFactoryTest {

  static EventManagerFactory eventManagerFactory;

  @BeforeAll
  static void beforeAll() {
    eventManagerFactory = new DefaultEventManagerFactory();
  }

  @Test
  void shouldCreateCheckpointEventManager() {
    final var checkpointEventManager1 = eventManagerFactory.getEventManager(Checkpoint.class);
    assertThat(checkpointEventManager1).isNotNull();
    final var checkpointEventManager2 = eventManagerFactory.getEventManager(Checkpoint.class);
    assertThat(checkpointEventManager2).isNotNull().isEqualTo(checkpointEventManager1);
  }

  @Test
  void shouldThrowExceptionWhenClassTypeIsNull() {
    assertThatThrownBy(() -> eventManagerFactory.getEventManager(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Class event type required");
  }
}
