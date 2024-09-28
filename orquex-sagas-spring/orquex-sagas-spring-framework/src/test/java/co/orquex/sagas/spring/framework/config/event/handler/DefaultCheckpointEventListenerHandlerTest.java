package co.orquex.sagas.spring.framework.config.event.handler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.orquex.sagas.core.flow.WorkflowStageExecutor;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCheckpointEventListenerHandlerTest {

  @Mock WorkflowStageExecutor workflowStageExecutor;

  @InjectMocks DefaultCheckpointEventListenerHandler checkpointEventListenerHandler;

  @Test
  void testHandleCheckpointCompletedWithOutgoing() {
    final var checkpoint = getCheckpoint(Status.COMPLETED, "outgoing");
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointCompletedWithoutOutgoing() {
    final var checkpoint = getCheckpoint(Status.COMPLETED);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointError() {
    final var checkpoint = getCheckpoint(Status.ERROR);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointCanceled() {
    final var checkpoint = getCheckpoint(Status.CANCELED);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointInProgress() {
    final var checkpoint = getCheckpoint(Status.IN_PROGRESS);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  private static Checkpoint getCheckpoint(Status status) {
    return getCheckpoint(status, null);
  }

  private static Checkpoint getCheckpoint(Status status, String outgoing) {
    final var stage =
        new Activity(
            "activity-id",
            "Activity name",
            Collections.emptyMap(),
            new StageConfiguration(),
            List.of(new ActivityTask("task-id")),
            false,
            outgoing,
            false);

    return Checkpoint.builder()
        .status(status)
        .flowId(UUID.randomUUID().toString())
        .correlationId(UUID.randomUUID().toString())
        .transactionId(UUID.randomUUID().toString())
        .incoming(stage)
        .outgoing(outgoing)
        .build();
  }
}
