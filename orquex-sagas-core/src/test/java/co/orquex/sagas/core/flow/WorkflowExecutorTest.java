package co.orquex.sagas.core.flow;

import static co.orquex.sagas.core.fixture.FlowFixture.getFlow;
import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TransactionFixture.getTransaction;
import static co.orquex.sagas.domain.transaction.Status.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.repository.CheckpointRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
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
  @Mock GlobalContext globalContext;
  @Mock CheckpointRepository checkpointRepository;

  WorkflowExecutor orchestratorExecutor;
  WorkflowExecutor executorWithCheckpoints;

  Flow simpleFlow;
  Flow resumableFlow;

  @BeforeEach
  void setUp() {
    final var executor = Executors.newSingleThreadExecutor();
    orchestratorExecutor =
        new WorkflowExecutor(
            flowRepository,
            transactionRepository,
            stageExecutor,
            compensationExecutor,
            executor,
            globalContext);
    executorWithCheckpoints =
        new WorkflowExecutor(
            flowRepository,
            transactionRepository,
            checkpointRepository,
            stageExecutor,
            compensationExecutor,
            executor,
            globalContext);
    simpleFlow = getFlow("flow-simple.json");
    resumableFlow = readValue("flow-resumable.json", Flow.class);
  }

  @Test
  void shouldExecuteWorkflow() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.empty());
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());
    StageResponse stageResponse = Mockito.mock(StageResponse.class);
    when(stageResponse.outgoing()).thenReturn("activity-stage", "");
    when(stageResponse.payload()).thenReturn(Collections.emptyMap());

    when(stageExecutor.execute(any(StageRequest.class))).thenReturn(stageResponse);

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    final var response = orchestratorExecutor.execute(executionRequest);
    assertThat(response).isNotNull();

    // Capture the transactions saved
    ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository, times(2)).save(transactionCaptor.capture());
    final var savedTransactions = transactionCaptor.getAllValues();
    assertThat(savedTransactions)
        .hasSize(2)
        .extracting(Transaction::status)
        .containsExactly(Status.IN_PROGRESS, Status.COMPLETED);

    verify(globalContext).remove(anyString());
  }

  @Test
  void shouldThrowWorkflowExceptionWhenCircularExecution() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.empty());
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
    verify(globalContext, never()).remove(anyString());
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
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.of(getTransaction()));
    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow '%s' with correlation ID '%s' has already been initiated."
                .formatted(FLOW_ID, CORRELATION_ID));
  }

  @Test
  void shouldThrowWorkflowExceptionWhenInterruptedException() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.empty());
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    when(stageExecutor.execute(any(StageRequest.class))).thenThrow(IllegalArgumentException.class);

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("An error occurred while executing flow '%s'.".formatted(FLOW_ID));
    verify(compensationExecutor).execute(any());
    verify(globalContext, never()).remove(anyString());
  }

  @Test
  void shouldCatchWorkflowException() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.empty());
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    when(stageExecutor.execute(any(StageRequest.class)))
        .thenThrow(new WorkflowException("Some message from an activity."));

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Some message from an activity.");
    verify(compensationExecutor).execute(any());
    verify(globalContext, never()).remove(anyString());
  }

  @Test
  void shouldThrowWorkflowExceptionWhenTimeoutException() {
    when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(FLOW_ID, CORRELATION_ID))
        .thenReturn(Optional.empty());
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());
    when(stageExecutor.execute(any(StageRequest.class)))
        .thenAnswer(getStageResponseAnswerTimeout());

    final var executionRequest = new ExecutionRequest(FLOW_ID, CORRELATION_ID);
    assertThatThrownBy(() -> orchestratorExecutor.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow '%s' timed out after PT1S.".formatted(FLOW_ID));
    verify(compensationExecutor).execute(any());
    verify(globalContext, never()).remove(anyString());
  }

  @SuppressWarnings("java:S2925")
  private static Answer<StageResponse> getStageResponseAnswerTimeout() {
    return invocation -> {
      TimeUnit.MILLISECONDS.sleep(1200);
      throw new IllegalStateException();
    };
  }

  @Test
  @DisplayName("Should not resume when resumeFromFailure is disabled")
  void shouldNotResumeWhenResumeFromFailureDisabled() {
    final var flowId = "flow-simple";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    final var transaction = getTransaction();

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));

    // Should throw exception because transaction already exists and resume is disabled
    assertThatThrownBy(() -> orchestratorExecutor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'flow-simple' with correlation ID 'correlation-id' has already been initiated.");
  }

  @Test
  @DisplayName("Should not resume when transaction is not in ERROR status")
  void shouldNotResumeWhenTransactionNotInErrorStatus() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    // Create a transaction with IN_PROGRESS status (not ERROR)
    final var transaction = getTransaction(flowId, Status.IN_PROGRESS);

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));

    // Should throw exception because transaction exists but is not in ERROR status
    assertThatThrownBy(() -> executorWithCheckpoints.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'resumable-flow' with correlation ID 'correlation-id' has already been initiated.");
  }

  @Test
  @DisplayName(
      "Should throw exception when resumeFromFailure enabled but checkpoint repository is null")
  void shouldThrowExceptionWhenCheckpointRepositoryIsNull() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    // Use ERROR status to trigger resume logic
    final var transaction = getTransaction(flowId, ERROR);

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));

    // Using executor without a checkpoint repository
    assertThatThrownBy(() -> orchestratorExecutor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'resumable-flow' with correlation id 'correlation-id' cannot be resumed from failure because checkpoint repository is not provided.");
  }

  @Test
  @DisplayName("Should throw exception when no checkpoint found for resume")
  void shouldThrowExceptionWhenNoCheckpointFound() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    // Use ERROR status to trigger resume logic
    final var transaction = getTransaction(flowId, ERROR);

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));
    when(checkpointRepository.findByTransactionId(transaction.transactionId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> executorWithCheckpoints.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'resumable-flow' with correlation id 'correlation-id' has no checkpoints to resume from.");
  }

  @Test
  @DisplayName("Should resume execution from checkpoint when resumeFromFailure is enabled")
  void shouldResumeExecutionFromCheckpoint() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    // Use ERROR status to trigger resume logic
    final var transaction = getTransaction(flowId, ERROR);
    final var checkpoint =
        new Checkpoint(
            transaction.transactionId(),
            null, // status
            flowId,
            correlationId,
            "stage-2",
            Map.of("key", "value"),
            Map.of("data", "resume-data"),
            null, // response
            null, // outgoing
            null, // incoming
            null, // createdAt
            null // updatedAt
            );

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));
    when(checkpointRepository.findByTransactionId(transaction.transactionId()))
        .thenReturn(Optional.of(checkpoint));
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    StageResponse stageResponse = Mockito.mock(StageResponse.class);
    when(stageResponse.outgoing()).thenReturn("");
    when(stageResponse.payload()).thenReturn(Map.of("result", "success"));
    when(stageExecutor.execute(any(StageRequest.class))).thenReturn(stageResponse);

    // Should not throw exception and should return the workflow response
    final var response = executorWithCheckpoints.execute(request);
    assertThat(response).isNotNull();
    assertThat(response.transactionId()).isNotNull();
    assertThat(response.payload()).isNotNull().containsEntry("result", "success");

    // Verify that global context was cleaned up (indicating successful completion)
    verify(globalContext).remove(anyString());
  }

  @Test
  @DisplayName("Should resume execution and use checkpoint metadata and payload")
  void shouldResumeWithCheckpointDataIntegrity() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    // Use ERROR status to trigger resume logic
    final var transaction = getTransaction(flowId, ERROR);
    final var checkpointMetadata =
        Map.<String, Serializable>of("checkpoint-key", "checkpoint-value");
    final var checkpointPayload = Map.<String, Serializable>of("checkpoint-data", "resume-payload");
    final var checkpoint =
        new Checkpoint(
            transaction.transactionId(),
            null, // status
            flowId,
            correlationId,
            "stage-2",
            checkpointMetadata,
            checkpointPayload,
            null, // response
            null, // outgoing
            null, // incoming
            null, // createdAt
            null // updatedAt
            );

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));
    when(checkpointRepository.findByTransactionId(transaction.transactionId()))
        .thenReturn(Optional.of(checkpoint));
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(AdditionalAnswers.returnsFirstArg());

    StageResponse stageResponse = Mockito.mock(StageResponse.class);
    when(stageResponse.outgoing()).thenReturn("");
    when(stageResponse.payload()).thenReturn(checkpointPayload);
    when(stageExecutor.execute(any(StageRequest.class))).thenReturn(stageResponse);

    final var response = executorWithCheckpoints.execute(request);

    // Verify that the stage request was called with checkpoint data
    verify(stageExecutor)
        .execute(
            argThat(
                stageRequest -> {
                  final var executionRequest = stageRequest.executionRequest();
                  return executionRequest.metadata().equals(checkpointMetadata)
                      && executionRequest.payload().equals(checkpointPayload);
                }));

    assertThat(response.payload()).isEqualTo(checkpointPayload);
  }
}
