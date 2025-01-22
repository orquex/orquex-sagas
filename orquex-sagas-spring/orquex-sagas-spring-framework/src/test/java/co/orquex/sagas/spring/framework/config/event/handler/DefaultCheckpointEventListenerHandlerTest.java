package co.orquex.sagas.spring.framework.config.event.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.flow.AsyncWorkflowStageExecutor;
import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCheckpointEventListenerHandlerTest {

  @Mock AsyncWorkflowStageExecutor workflowStageExecutor;
  @Mock CompensationExecutor compensationExecutor;
  @Mock FlowRepository flowRepository;
  @Mock TransactionRepository transactionRepository;

  @InjectMocks DefaultCheckpointEventListenerHandler checkpointEventListenerHandler;

  @Test
  void testHandleCheckpointCompletedWithOutgoing() {
    final var checkpoint = getCheckpoint(Status.COMPLETED, "outgoing");
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointCompletedWithoutOutgoing() {
    final var transaction = Mockito.mock(Transaction.class);
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
    final var checkpoint = getCheckpoint(Status.COMPLETED);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor, never()).execute(checkpoint);
    verify(transactionRepository).save(transaction);
  }

  @Test
  void testHandleCheckpointErrorWhenAllOrNothing() {
    final var flow = Mockito.mock(Flow.class, Mockito.RETURNS_DEEP_STUBS);
    when(flow.configuration().allOrNothing()).thenReturn(true);
    when(flowRepository.findById(anyString())).thenReturn(Optional.of(flow));
    final var transaction = Mockito.mock(Transaction.class);
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
    final var checkpoint = getCheckpoint(Status.ERROR);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(compensationExecutor).execute(anyString());
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointErrorWhenOutgoingNotPresent() {
    final var flow = Mockito.mock(Flow.class, Mockito.RETURNS_DEEP_STUBS);
    when(flowRepository.findById(anyString())).thenReturn(Optional.of(flow));
    final var transaction = Mockito.mock(Transaction.class);
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
    final var checkpoint = getCheckpoint(Status.ERROR);
    checkpointEventListenerHandler.handle(checkpoint);
    verify(transactionRepository).save(transaction);
    verify(compensationExecutor).execute(anyString());
    verify(workflowStageExecutor, never()).execute(checkpoint);
  }

  @Test
  void testHandleCheckpointErrorWhenOutgoingNotPresentAndTransactionNotFound() {
    final var flow = Mockito.mock(Flow.class, Mockito.RETURNS_DEEP_STUBS);
    when(flowRepository.findById(anyString())).thenReturn(Optional.of(flow));
    when(transactionRepository.findById(anyString())).thenReturn(Optional.empty());
    final var checkpoint = getCheckpoint(Status.ERROR);
    assertThatThrownBy(() -> checkpointEventListenerHandler.handle(checkpoint))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Transaction '%s' not found.".formatted(checkpoint.transactionId()));
  }

  @Test
  void testHandleCheckpointErrorWhenOutgoingPresent() {
    final var flow = Mockito.mock(Flow.class, Mockito.RETURNS_DEEP_STUBS);
    when(flowRepository.findById(anyString())).thenReturn(Optional.of(flow));
    final var checkpoint = getCheckpoint(Status.ERROR, "outgoing");
    checkpointEventListenerHandler.handle(checkpoint);
    verify(workflowStageExecutor).execute(checkpoint);
    verify(compensationExecutor, never()).execute(anyString());
  }

  @Test
  void testHandleCheckpointErrorWhenFlowNotFound() {
    when(flowRepository.findById(anyString())).thenReturn(Optional.empty());
    final var checkpoint = getCheckpoint(Status.ERROR);
    assertThatThrownBy(() -> checkpointEventListenerHandler.handle(checkpoint))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow '%s' not found.".formatted(checkpoint.flowId()));
  }

  @Test
  void testHandleCheckpointCanceled() {
    final var checkpoint = getCheckpoint(Status.CANCELED);
    final var transaction = Mockito.mock(Transaction.class);
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
    checkpointEventListenerHandler.handle(checkpoint);
    verify(transactionRepository).save(transaction);
    verify(compensationExecutor).execute(anyString());
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
