package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.StageRequestFixture.getStageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.fixture.ExecutionRequestFixture;
import co.orquex.sagas.core.stage.strategy.StageProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.StrategyResponse;
import co.orquex.sagas.core.stage.strategy.impl.decorator.EventHandlerProcessingStrategy;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.*;
import co.orquex.sagas.domain.transaction.Checkpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutableStageTest {

  @Mock private WorkflowEventPublisher workflowEventPublisher;
  @Mock private StageProcessingStrategy<Activity> activityStrategy;
  @Mock private StageProcessingStrategy<Evaluation> evaluationStrategy;
  @Captor private ArgumentCaptor<EventMessage<Checkpoint>> eventMessageCaptor;
  private DefaultStageExecutor executableStage;

  @BeforeEach
  void setUp() {
    final var activityStrategyDecorator =
        new EventHandlerProcessingStrategy<>(activityStrategy, workflowEventPublisher);
    final var evaluationStrategyDecorator =
        new EventHandlerProcessingStrategy<>(evaluationStrategy, workflowEventPublisher);
    executableStage =
        new DefaultStageExecutor(activityStrategyDecorator, evaluationStrategyDecorator);
  }

  @Test
  void shouldExecuteActivityStrategyImplementation() {
    when(activityStrategy.process(anyString(), any(Activity.class), any(ExecutionRequest.class)))
        .thenReturn(StrategyResponse.builder().outgoing("test-outgoing").build());

    final var activity = readValue("stage-activity-simple.json", Activity.class);
    final var request = ExecutionRequestFixture.getExecutionRequest();
    executableStage.execute(getStageRequest(activity, request));
    verify(activityStrategy).process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy, never())
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
    // Capture the event message sent
    verify(workflowEventPublisher, times(2)).publish(eventMessageCaptor.capture());
    final var checkpoint = eventMessageCaptor.getValue();
    assertThat(checkpoint).isNotNull();
  }

  @Test
  void shouldExecuteEvaluationStrategyImplementation() {
    when(evaluationStrategy.process(
            anyString(), any(Evaluation.class), any(ExecutionRequest.class)))
        .thenReturn(StrategyResponse.builder().outgoing("test-outgoing").build());

    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var request = ExecutionRequestFixture.getExecutionRequest();
    executableStage.execute(getStageRequest(evaluation, request));
    verify(activityStrategy, never())
        .process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy)
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
    // Capture the event message sent
    verify(workflowEventPublisher, times(2)).publish(eventMessageCaptor.capture());
    final var checkpoint = eventMessageCaptor.getValue();
    assertThat(checkpoint).isNotNull();
  }

  @Test
  void shouldSendErrorWhenStrategyFail() {
    when(evaluationStrategy.process(
            anyString(), any(Evaluation.class), any(ExecutionRequest.class)))
        .thenThrow(new WorkflowException("some runtime exception"));

    executableStage.execute(getStageRequest(mock(Evaluation.class), mock(ExecutionRequest.class)));
    verify(activityStrategy, never())
        .process(anyString(), any(Activity.class), any(ExecutionRequest.class));
    verify(evaluationStrategy)
        .process(anyString(), any(Evaluation.class), any(ExecutionRequest.class));
    // Capture the event message sent
    verify(workflowEventPublisher, times(2)).publish(eventMessageCaptor.capture());
    final var eventMessage = eventMessageCaptor.getValue();
    assertThat(eventMessage).isNotNull();
    assertThat(eventMessage.hasError()).isTrue();
  }
}
