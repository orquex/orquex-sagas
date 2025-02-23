package co.orquex.sagas.core.flow;

import static co.orquex.sagas.core.fixture.FlowFixture.getFlow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncWorkflowStageExecutorTest {

  static final String FLOW_ID = "flow-simple";
  static final String CORRELATION_ID = UUID.randomUUID().toString();

  @Mock WorkflowEventPublisher workflowEventPublisher;
  @Mock FlowRepository flowRepository;
  @Mock TransactionRepository transactionRepository;

  @InjectMocks AsyncWorkflowStageExecutor stageExecutor;

  @Captor ArgumentCaptor<EventMessage<StageRequest>> eventMessageCaptor;

  @Test
  void shouldExecuteAsyncWorkflowStage() {
    final var checkpoint =
        Checkpoint.builder()
            .transactionId(UUID.randomUUID().toString())
            .flowId(FLOW_ID)
            .correlationId(CORRELATION_ID)
            .outgoing("activity-stage")
            .build();
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(true);
    final var simpleFlow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));

    stageExecutor.execute(checkpoint);

    verify(transactionRepository).existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID);
    verify(flowRepository).findById(FLOW_ID);
    verify(workflowEventPublisher).publish(any());
  }

  @Test
  void shouldThrowWorkflowExceptionWhenOutgoingCheckpointIsNull() {
    final var checkpoint = Checkpoint.builder().flowId(FLOW_ID).correlationId(FLOW_ID).build();

    stageExecutor.execute(checkpoint);

    verify(transactionRepository, never()).existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID);
    verify(flowRepository, never()).findById(FLOW_ID);
    verify(workflowEventPublisher, never()).publish(any());
  }

  @Test
  void shouldThrowWorkflowExceptionWhenTransactionNotFound() {
    final var checkpoint =
        Checkpoint.builder()
            .flowId(FLOW_ID)
            .correlationId(CORRELATION_ID)
            .outgoing("activity-stage")
            .build();
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);

    stageExecutor.execute(checkpoint);

    verify(transactionRepository).existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID);
    verify(flowRepository, never()).findById(FLOW_ID);
    verify(workflowEventPublisher).publish(eventMessageCaptor.capture());
    final var eventMessage = eventMessageCaptor.getValue();
    assertThat(eventMessage.message()).isNull();
    assertThat(eventMessage.error())
        .isNotNull()
        .extracting("message")
        .asString()
        .contains(
            "Transaction not found by flowId '%s' and correlation ID '%s'."
                .formatted(FLOW_ID, CORRELATION_ID));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenFlowNotFound() {
    final var checkpoint =
        Checkpoint.builder()
            .flowId(FLOW_ID)
            .correlationId(CORRELATION_ID)
            .outgoing("activity-stage")
            .build();
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(true);
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.empty());

    stageExecutor.execute(checkpoint);

    verify(transactionRepository).existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID);
    verify(flowRepository).findById(FLOW_ID);
    verify(workflowEventPublisher).publish(eventMessageCaptor.capture());
    final var eventMessage = eventMessageCaptor.getValue();
    assertThat(eventMessage.message()).isNull();
    assertThat(eventMessage.error())
        .isNotNull()
        .extracting("message")
        .asString()
        .contains(
            "Flow not found by '%s' when executing checkpoint stage with correlation ID '%s'."
                .formatted(FLOW_ID, CORRELATION_ID));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenStageNotFound() {
    final var checkpoint =
        Checkpoint.builder()
            .transactionId(UUID.randomUUID().toString())
            .flowId(FLOW_ID)
            .correlationId(CORRELATION_ID)
            .outgoing("activity-not-exists")
            .build();
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(true);
    final var simpleFlow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));

    stageExecutor.execute(checkpoint);

    verify(transactionRepository).existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID);
    verify(flowRepository).findById(FLOW_ID);
    verify(workflowEventPublisher).publish(eventMessageCaptor.capture());
    final var eventMessage = eventMessageCaptor.getValue();
    assertThat(eventMessage.message()).isNull();
    assertThat(eventMessage.error())
        .isNotNull()
        .extracting("message")
        .asString()
        .contains("Stage 'activity-not-exists' not found in flow 'flow-simple'.");
  }
}
