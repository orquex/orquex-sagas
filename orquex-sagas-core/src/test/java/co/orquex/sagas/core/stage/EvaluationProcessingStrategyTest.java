package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TaskFixture.getTask;
import static co.orquex.sagas.domain.stage.Evaluation.RESULT;
import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.task.Task;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationProcessingStrategyTest {

  @Mock private Registry<TaskExecutor> taskExecutorRegistry;
  @Mock private TaskRepository taskRepository;
  private EvaluationProcessingStrategy strategy;
  @Mock TaskExecutor taskExecutor;
  private ExecutionRequest executionRequest;
  private String transactionId;

  @BeforeEach
  void setUp() {
    strategy = new EvaluationProcessingStrategy(taskExecutorRegistry, taskRepository);
    executionRequest =
        new ExecutionRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    transactionId = UUID.randomUUID().toString();
  }

  @Test
  void shouldReturnDefaultOutgoingWhenConditionsNotMatched() {
    final var simpleTask = getTask("simple-evaluation-task");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, false));
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(simpleTask));
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var response = strategy.process(transactionId, evaluation, executionRequest);
    assertThat(response).isNotNull();
    assertThat(response.outgoing()).isEqualTo(evaluation.getDefaultOutgoing());
  }

  @Test
  void shouldReturnTheTaskOutgoingWhenResultTrue() {
    final var simpleTask = getTask("simple-evaluation-task");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, false))
        .thenReturn(Map.of(RESULT, true));
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(simpleTask));
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var response = strategy.process(transactionId, evaluation, executionRequest);
    assertThat(response).isNotNull();
    assertThat(response.outgoing()).isEqualTo("outgoing-test-2");
  }
}
