package co.orquex.sagas.core.flow;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.stage.DefaultAsyncStageExecutor;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.List;
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
  private static AsyncWorkflowExecutor executor;
  private static Flow simpleFlow;

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
    simpleFlow = readValue("flow-simple.json", Flow.class);
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
    when(transactionRepository.existsByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(true);
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
    when(transactionRepository.existsByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(false);
    final var transaction = new Transaction();
    transaction.setTransactionId(UUID.randomUUID().toString());
    when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
    executor.execute(request);
    verify(stageExecutor, timeout(100)).execute(any(StageRequest.class));
  }
}
