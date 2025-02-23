package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.StageRequestFixture.getStageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.fixture.ExecutionRequestFixture;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.StageResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultStageExecutorTest {

  @Mock StageProcessingStrategy<Activity> activityStrategy;
  @Mock StageProcessingStrategy<Evaluation> evaluationStrategy;

  DefaultStageExecutor stageExecutor;

  @BeforeEach
  void setUp() {
    stageExecutor = new DefaultStageExecutor(activityStrategy, evaluationStrategy);
  }

  @Test
  void shouldExecuteActivityStrategyImplementation() {
    final var stageResponse =
        StageResponse.builder()
            .transactionId("test-transaction")
            .outgoing("test-outgoing")
            .payload(Collections.emptyMap())
            .build();
    when(activityStrategy.process(anyString(), any(Activity.class), any(ExecutionRequest.class)))
        .thenReturn(stageResponse);

    final var activity = readValue("stage-activity-simple.json", Activity.class);
    final var request = ExecutionRequestFixture.getExecutionRequest();
    final var response = stageExecutor.execute(getStageRequest(activity, request));

    assertThat(response)
        .isNotNull()
        .returns("test-outgoing", StageResponse::outgoing)
        .returns("test-transaction", StageResponse::transactionId)
        .returns(Collections.emptyMap(), StageResponse::payload);
    verify(activityStrategy).process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy, never())
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteEvaluationStrategyImplementation() {
    final var stageResponse =
        StageResponse.builder()
            .transactionId("test-transaction")
            .outgoing("test-outgoing")
            .payload(Collections.emptyMap())
            .build();
    when(evaluationStrategy.process(
            anyString(), any(Evaluation.class), any(ExecutionRequest.class)))
        .thenReturn(stageResponse);

    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var request = ExecutionRequestFixture.getExecutionRequest();
    final var response = stageExecutor.execute(getStageRequest(evaluation, request));
    assertThat(response)
        .isNotNull()
        .returns("test-outgoing", StageResponse::outgoing)
        .returns("test-transaction", StageResponse::transactionId)
        .returns(Collections.emptyMap(), StageResponse::payload);

    verify(activityStrategy, never())
        .process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy)
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
  }

  @Test
  void shouldThrowErrorWhenStrategyFail() {
    when(evaluationStrategy.process(
            anyString(), any(Evaluation.class), any(ExecutionRequest.class)))
        .thenThrow(new WorkflowException("some runtime exception"));

    final var stageRequest = getStageRequest(mock(Evaluation.class), mock(ExecutionRequest.class));
    assertThatThrownBy(() -> stageExecutor.execute(stageRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("some runtime exception");

    verify(activityStrategy, never())
        .process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy)
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
  }
}
