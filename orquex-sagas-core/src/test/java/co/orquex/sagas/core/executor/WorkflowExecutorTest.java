package co.orquex.sagas.core.executor;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.DefaultStageExecutor;
import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutorTest {

  @Mock private DefaultStageExecutor stageExecutor;
  @Mock private FlowRepository flowRepository;
  @Mock private TransactionRepository transactionRepository;
  private static WorkflowExecutor executor;
  private static Flow simpleFlow;

  @BeforeEach
  void setUp() {
    // Setting default stage executor
    when(stageExecutor.getId()).thenReturn(StageConfiguration.DEFAULT_IMPLEMENTATION);
    final var stageExecutorRegistry = InMemoryStageExecutorRegistry.of(List.of(stageExecutor));
    // Create the event manager factory to get the StageRequest
    final var eventManagerFactory = new DefaultEventManagerFactory();
    // Create and get the StageRequest event handler to send stages and add a listener
    eventManagerFactory
        .getEventManager(StageRequest.class)
        .addListener(new DefaultStageEventListener(stageExecutorRegistry));
    // Create the workflow event publisher
    final var workflowEventPublisher = new DefaultWorkflowEventPublisher(eventManagerFactory);
    executor = new WorkflowExecutor(workflowEventPublisher, flowRepository, transactionRepository);
    simpleFlow = readValue("flow-simple.json", Flow.class);
  }

  @Test
  void shouldThrowExceptionWhenRequestNull() {
    assertThatThrownBy(() -> executor.execute(null))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Execution request required");
  }

  @Test
  void shouldThrowExceptionWhenFlowNotFound() {
    final var request = new ExecutionRequest("flow-id", "correlation-id");
    when(flowRepository.findById(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> executor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Flow 'flow-id' not found.");
  }

  @Test
  void shouldThrowExceptionWhenTransactionAlreadyStarted() {
    final var flowId = "flow-simple";
    final var request = new ExecutionRequest(flowId, "correlation-id");
    when(flowRepository.findById(flowId)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.existByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(true);
    assertThatThrownBy(() -> executor.execute(request))
        .isInstanceOf(WorkflowException.class)
        .hasMessage(
            "Flow 'Simple Flow' with correlation id 'correlation-id' has already been initiated");
  }

  @Test
  void shouldExecuteFlow() {
    final var flowId = "flow-simple";
    final var request = new ExecutionRequest(flowId, "correlation-id");
    when(flowRepository.findById(flowId)).thenReturn(Optional.of(simpleFlow));
    when(transactionRepository.existByFlowIdAndCorrelationId(anyString(), anyString()))
        .thenReturn(false);
    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(Transaction.builder().transactionId(UUID.randomUUID().toString()).build());
    executor.execute(request);
    verify(stageExecutor, timeout(100)).execute(any(StageRequest.class));
  }
}
