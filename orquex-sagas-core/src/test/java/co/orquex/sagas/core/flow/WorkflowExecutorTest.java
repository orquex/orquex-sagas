package co.orquex.sagas.core.flow;

import static co.orquex.sagas.core.fixture.FlowFixture.getFlow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutorTest {

  static final String FLOW_ID = "flow-simple";
  static final String CORRELATION_ID = UUID.randomUUID().toString();
  @Mock FlowRepository flowRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock StageExecutor stageExecutor;
  @Mock CompensationExecutor compensationExecutor;

  WorkflowExecutor orchestratorExecutor;

  @BeforeEach
  void setUp() {
    final var executor = Executors.newSingleThreadExecutor();
    orchestratorExecutor =
        new WorkflowExecutor(
            flowRepository, transactionRepository, stageExecutor, compensationExecutor, executor);
  }

  @Test
  void shouldExecuteWorkflow() {
    final var flow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());
    StageResponse stageResponse = Mockito.mock(StageResponse.class);
    when(stageResponse.outgoing()).thenReturn("activity-stage", "");
    when(stageResponse.payload()).thenReturn(Collections.emptyMap());

    when(stageExecutor.execute(any(StageRequest.class))).thenReturn(stageResponse);

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    final var response = orchestratorExecutor.execute(executionRequest);
    assertThat(response).isNotNull();
  }

  @Test
  void shouldThrowWorkflowExceptionWhenCircularExecution() {
    final var flow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());
    StageResponse stageResponse = Mockito.mock(StageResponse.class);
    final var circularStage = "activity-stage";
    when(stageResponse.outgoing()).thenReturn(circularStage, circularStage);
    when(stageResponse.payload()).thenReturn(Collections.emptyMap());

    when(stageExecutor.execute(any(StageRequest.class))).thenReturn(stageResponse);

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Circular execution detected in flow '%s' at stage '%s'."
                .formatted(FLOW_ID, circularStage));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenExecutionRequestIsNull() {
    assertThatThrownBy(() -> orchestratorExecutor.execute(null))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Execution request required.");
  }

  @Test
  void shouldThrowWorkflowExceptionWhenFlowNotFound() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.empty());
    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow '%s' not found.".formatted(FLOW_ID));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenFlowWithCorrelationIdAlreadyExists() {
    final var flow = Mockito.mock(Flow.class);
    when(flow.id()).thenReturn(FLOW_ID);
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(true);
    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow '%s' with correlation ID '%s' has already been initiated."
                .formatted(FLOW_ID, CORRELATION_ID));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenInterruptedException() {
    final var flow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    when(stageExecutor.execute(any(StageRequest.class))).thenThrow(IllegalArgumentException.class);

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("An error occurred while executing flow '%s'.".formatted(FLOW_ID));
    verify(compensationExecutor).execute(any());
  }

  @Test
  void shouldCatchWorkflowException() {
    final var flow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    when(stageExecutor.execute(any(StageRequest.class)))
        .thenThrow(new WorkflowException("Some message from an activity."));

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Some message from an activity.");
    verify(compensationExecutor).execute(any());
  }

  @Test
  void shouldThrowWorkflowExceptionWhenTimeoutException() {
    final var flow = getFlow("flow-simple.json");
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));
    when(transactionRepository.existsByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());
    when(stageExecutor.execute(any(StageRequest.class)))
        .thenAnswer(getStageResponseAnswerTimeout());

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow '%s' timed out after PT1S.".formatted(FLOW_ID));
    verify(compensationExecutor).execute(any());
  }

  @SuppressWarnings("java:S2925")
  private static Answer<StageResponse> getStageResponseAnswerTimeout() {
    return invocation -> {
      TimeUnit.MILLISECONDS.sleep(1200);
      throw new IllegalStateException();
    };
  }
}
