package co.orquex.sagas.core.executor;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.ExecutableStage;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutorTest {

  @Mock private ExecutableStage executableStage;
  @Mock private FlowRepository flowRepository;
  @Mock private TransactionRepository transactionRepository;
  private static WorkflowExecutor executor;
  private static Flow simpleFlow;

  @BeforeEach
  void setUp() {
    // Create the event manager to send stages
    final var stageRequestEventManager = new EventManager<StageRequest>();
    stageRequestEventManager.addListener(new DefaultStageEventListener(executableStage));
    executor = new WorkflowExecutor(stageRequestEventManager, flowRepository, transactionRepository);
    simpleFlow = readValue("flow-simple.json", Flow.class);
  }

  @Test
  void shouldThrowExceptionWhenRequestNull() {
    assertThatThrownBy(() -> executor.execute(null))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("execution executionRequest required");
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
            "flow 'Simple Flow' with correlation id 'correlation-id' has already been initiated");
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
    verify(executableStage, timeout(100)).execute(any(StageRequest.class));
  }
}
