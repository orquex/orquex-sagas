package co.orquex.sagas.spring.framework.config.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.stage.StageType;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.spring.framework.config.event.handler.CheckpointEventListenerHandler;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCheckpointEventListenerTest {

  @Mock CheckpointEventListenerHandler handler;

  @InjectMocks DefaultCheckpointEventListener defaultCheckpointEventListener;

  @Test
  void testOnMessage() {
    // Given
    final var checkpoint = getCheckpoint();
    final var event = new EventMessage<>(checkpoint);
    // When
    defaultCheckpointEventListener.onMessage(event);
    // Then
    verify(handler).handle(checkpoint);
  }

  @Test
  void testOnError() {
    // Given
    final var checkpoint = getCheckpoint();
    final var error = Error.builder().build();
    final var event = new EventMessage<>(checkpoint, error);
    // When
    defaultCheckpointEventListener.onError(event);
    // Then
    verify(handler).handle(any(Checkpoint.class));
  }

  private static Checkpoint getCheckpoint() {
    final var stage =
        new Stage(
            StageType.activity.name(),
            "activity-id",
            "Activity name",
            Collections.emptyMap(),
            new StageConfiguration());

    return Checkpoint.builder()
        .flowId(UUID.randomUUID().toString())
        .correlationId(UUID.randomUUID().toString())
        .transactionId(UUID.randomUUID().toString())
        .incoming(stage)
        .build();
  }
}
