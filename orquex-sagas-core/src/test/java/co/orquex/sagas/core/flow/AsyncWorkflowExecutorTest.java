package co.orquex.sagas.core.flow;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TransactionFixture.getTransaction;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.stage.DefaultAsyncStageExecutor;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.domain.api.repository.CheckpointRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Transaction;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncWorkflowExecutorTest {

  @Mock private DefaultAsyncStageExecutor stageExecutor;
  @Mock private FlowRepository flowRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private CheckpointRepository checkpointRepository;
  private static AsyncWorkflowExecutor executor;
  private static AsyncWorkflowExecutor executorWithCheckpoints;
  private static Flow simpleFlow;
  private static Flow resumableFlow;

  @BeforeEach
  void setUp() {
    // Setting default stage executor
    when(stageExecutor.getKey()).thenReturn(StageConfiguration.DEFAULT_IMPLEMENTATION);
    final var stageExecutorRegistry = InMemoryStageExecutorRegistry.of(List.of(stageExecutor));
    // Create the event manager factory to get the StageRequest
    final var eventManagerFactory = new DefaultEventManagerFactory();
    // Create and get the StageRequest event handler to send stages and add a listener
    eventManagerFactory
        .getEventManager(StageRequest.class)
        .addListener(new DefaultStageEventListener(stageExecutorRegistry));
    // Create the workflow event publisher
    final var workflowEventPublisher = new DefaultWorkflowEventPublisher(eventManagerFactory);
    executor =
        new AsyncWorkflowExecutor(workflowEventPublisher, flowRepository, transactionRepository);
    executorWithCheckpoints =
        new AsyncWorkflowExecutor(
            workflowEventPublisher, flowRepository, transactionRepository, checkpointRepository);
    simpleFlow = readValue("flow-simple.json", Flow.class);
    resumableFlow = readValue("flow-resumable.json", Flow.class);
  }

  @Test
  void shouldThrowExceptionWhenRequestNull() {
    assertThatThrownBy(() -> executor.execute(null))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Execution request required.");
  }

  @Test
  @DisplayName("Should throw exception when flow id is null")
  void shouldThrowExceptionWhenFlowNotFound() {
    final var request = new ExecutionRequest("flow-id", "correlation-id");
    when(flowRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> executor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow 'flow-id' not found.");
  }

  @Test
  @DisplayName("Should throw exception when transaction already started")
  void shouldThrowExceptionWhenTransactionAlreadyStarted() {
    final var flowId = "flow-simple";
    final var request = new ExecutionRequest(flowId, "correlation-id");
    when(flowRepository.findById(flowId)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(Optional.of(getTransaction()));

    assertThatThrownBy(() -> executor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'flow-simple' with correlation id 'correlation-id' has already been initiated.");
  }

  @Test
  @DisplayName("Should execute simple flow")
  void shouldExecuteSimpleFlow() {
    final var flowId = "flow-simple";
    final var request = new ExecutionRequest(flowId, "correlation-id");
    when(flowRepository.findById(flowId)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(Optional.empty());
    final var transaction = new Transaction();
    transaction.setTransactionId(UUID.randomUUID().toString());
    when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

    executor.execute(request);

    verify(stageExecutor, timeout(100)).execute(any(StageRequest.class));
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
    assertThatThrownBy(() -> executor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'flow-simple' with correlation id 'correlation-id' has already been initiated.");
  }

  @Test
  @DisplayName(
      "Should throw exception when resumeFromFailure enabled but checkpoint repository is null")
  void shouldThrowExceptionWhenCheckpointRepositoryIsNull() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    final var transaction = getTransaction();

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));

    // Using executor without checkpoint repository
    assertThatThrownBy(() -> executor.execute(request))
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
    final var transaction = getTransaction();

    when(flowRepository.findById(flowId)).thenReturn(Optional.of(resumableFlow));
    when(transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId))
        .thenReturn(Optional.of(transaction));
    when(checkpointRepository.findByTransactionId(transaction.getTransactionId()))
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
    final var transaction = getTransaction();
    final var checkpoint =
        new Checkpoint(
            transaction.getTransactionId(),
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
    when(checkpointRepository.findByTransactionId(transaction.getTransactionId()))
        .thenReturn(Optional.of(checkpoint));

    // Should not throw exception and should execute the stage from checkpoint
    assertThatCode(() -> executorWithCheckpoints.execute(request)).doesNotThrowAnyException();

    // Verify that stage executor was called (once for resume execution)
    verify(stageExecutor, timeout(100).times(1)).execute(any(StageRequest.class));
  }

  @Test
  @DisplayName("Should resume execution and merge checkpoint metadata and payload")
  void shouldResumeWithCheckpointDataIntegrity() {
    final var flowId = "resumable-flow";
    final var correlationId = "correlation-id";
    final var request = new ExecutionRequest(flowId, correlationId);
    final var transaction = getTransaction();
    final var checkpointMetadata =
        Map.<String, Serializable>of("checkpoint-key", "checkpoint-value");
    final var checkpointPayload = Map.<String, Serializable>of("checkpoint-data", "resume-payload");
    final var checkpoint =
        new Checkpoint(
            transaction.getTransactionId(),
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
    when(checkpointRepository.findByTransactionId(transaction.getTransactionId()))
        .thenReturn(Optional.of(checkpoint));

    executorWithCheckpoints.execute(request);

    // Verify that the stage request contains the checkpoint metadata and payload
    verify(stageExecutor, timeout(100))
        .execute(
            argThat(
                stageRequest -> {
                  final var executionRequest = stageRequest.executionRequest();
                  return executionRequest.metadata().equals(checkpointMetadata)
                      && executionRequest.payload().equals(checkpointPayload);
                }));
  }
}
