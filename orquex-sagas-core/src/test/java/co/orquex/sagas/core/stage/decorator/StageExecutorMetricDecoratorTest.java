package co.orquex.sagas.core.stage.decorator;

import static co.orquex.sagas.core.fixture.ActivityFixture.getSimpleActivity;
import static co.orquex.sagas.core.fixture.ActivityTaskFixture.getSimpleActivityTask;
import static co.orquex.sagas.core.fixture.ExecutionRequestFixture.getExecutionRequest;
import static co.orquex.sagas.core.fixture.StageRequestFixture.getStageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.event.metric.StageCompletedEvent;
import co.orquex.sagas.domain.event.metric.StageFailedEvent;
import co.orquex.sagas.domain.event.metric.StageStartedEvent;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StageExecutorMetricDecorator Tests")
class StageExecutorMetricDecoratorTest {

  private static final String STAGE_ID = "test-stage";
  private static final String EXECUTOR_KEY = "test-executor";
  public static final String ACTIVITY_TASK_ID = "test-activity-task";

  @Mock private StageExecutor delegate;
  @Mock private WorkflowEventPublisher eventPublisher;

  private StageExecutorMetricDecorator decorator;
  private StageRequest stageRequest;
  private ExecutionRequest executionRequest;

  @BeforeEach
  void setUp() {
    decorator = new StageExecutorMetricDecorator(delegate, eventPublisher);

    executionRequest = getExecutionRequest();
    final var activity =
        getSimpleActivity(STAGE_ID, List.of(getSimpleActivityTask(ACTIVITY_TASK_ID)), false, false);
    stageRequest = getStageRequest(activity, executionRequest);
  }

  @Test
  @DisplayName("should execute stage successfully and publish started and completed events")
  void shouldExecuteStageSuccessfullyAndPublishEvents() {
    // Arrange
    final var expectedResponse =
        new StageResponse(stageRequest.transactionId(), Collections.emptyMap(), "next-stage");
    when(delegate.execute(stageRequest)).thenReturn(expectedResponse);

    // Act
    final var actualResponse = decorator.execute(stageRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(stageRequest.transactionId(), StageResponse::transactionId)
        .returns("next-stage", StageResponse::outgoing)
        .returns(Collections.emptyMap(), StageResponse::payload);

    verify(delegate).execute(stageRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageStartedEvent stageStarted
                        && stageStarted.flowId().equals(executionRequest.flowId())
                        && stageStarted.correlationId().equals(executionRequest.correlationId())
                        && stageStarted.transactionId().equals(stageRequest.transactionId())
                        && stageStarted.stage().equals(STAGE_ID)
                        && stageStarted.timestamp() != null));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageCompletedEvent stageCompleted
                        && stageCompleted.flowId().equals(executionRequest.flowId())
                        && stageCompleted.correlationId().equals(executionRequest.correlationId())
                        && stageCompleted.transactionId().equals(stageRequest.transactionId())
                        && stageCompleted.stage().equals(STAGE_ID)
                        && stageCompleted.timestamp() != null
                        && stageCompleted.duration() != null));
  }

  @Test
  @DisplayName("should publish failed event when stage execution throws exception")
  void shouldPublishFailedEventWhenStageExecutionThrowsException() {
    // Arrange
    final var exception = new WorkflowException("Stage execution failed");
    when(delegate.execute(stageRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(stageRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Stage execution failed");

    verify(delegate).execute(stageRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageStartedEvent stageStarted
                        && stageStarted.flowId().equals(executionRequest.flowId())
                        && stageStarted.correlationId().equals(executionRequest.correlationId())
                        && stageStarted.transactionId().equals(stageRequest.transactionId())
                        && stageStarted.stage().equals(STAGE_ID)));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageFailedEvent stageFailed
                        && stageFailed.flowId().equals(executionRequest.flowId())
                        && stageFailed.correlationId().equals(executionRequest.correlationId())
                        && stageFailed.transactionId().equals(stageRequest.transactionId())
                        && stageFailed.stage().equals(STAGE_ID)
                        && stageFailed.timestamp() != null
                        && stageFailed.duration() != null));
    verify(eventPublisher, never())
        .publish(
            argThat((EventMessage<?> event) -> event.message() instanceof StageCompletedEvent));
  }

  @Test
  @DisplayName("should continue stage execution even if started event publishing fails")
  void shouldContinueStageExecutionEvenIfStartedEventPublishingFails() {
    // Arrange
    final var expectedResponse =
        new StageResponse(stageRequest.transactionId(), Collections.emptyMap(), "next-stage");
    when(delegate.execute(stageRequest)).thenReturn(expectedResponse);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof StageStartedEvent));

    // Act
    final var actualResponse = decorator.execute(stageRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(stageRequest.transactionId(), StageResponse::transactionId);
    verify(delegate).execute(stageRequest);
    verify(eventPublisher)
        .publish(
            argThat((EventMessage<?> event) -> event.message() instanceof StageCompletedEvent));
  }

  @Test
  @DisplayName("should continue stage execution even if completed event publishing fails")
  void shouldContinueStageExecutionEvenIfCompletedEventPublishingFails() {
    // Arrange
    final var expectedResponse =
        new StageResponse(stageRequest.transactionId(), Collections.emptyMap(), "next-stage");
    when(delegate.execute(stageRequest)).thenReturn(expectedResponse);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(
            argThat((EventMessage<?> event) -> event.message() instanceof StageCompletedEvent));

    // Act
    final var actualResponse = decorator.execute(stageRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(stageRequest.transactionId(), StageResponse::transactionId);
    verify(delegate).execute(stageRequest);
  }

  @Test
  @DisplayName("should rethrow exception even if failed event publishing fails")
  void shouldRethrowExceptionEvenIfFailedEventPublishingFails() {
    // Arrange
    final var exception = new WorkflowException("Stage execution failed");
    when(delegate.execute(stageRequest)).thenThrow(exception);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof StageFailedEvent));

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(stageRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Stage execution failed");

    verify(delegate).execute(stageRequest);
  }

  @Test
  @DisplayName("should measure execution duration correctly for successful execution")
  void shouldMeasureExecutionDurationCorrectlyForSuccessfulExecution() throws InterruptedException {
    // Arrange
    final var expectedResponse =
        new StageResponse(stageRequest.transactionId(), Collections.emptyMap(), "next-stage");
    when(delegate.execute(stageRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              return expectedResponse;
            });

    // Act
    final var actualResponse = decorator.execute(stageRequest);

    // Assert
    assertThat(actualResponse).isNotNull();
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageCompletedEvent stageCompleted
                        && stageCompleted.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should measure execution duration correctly for failed execution")
  void shouldMeasureExecutionDurationCorrectlyForFailedExecution() throws InterruptedException {
    // Arrange
    when(delegate.execute(stageRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              throw new WorkflowException("Stage execution failed");
            });

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(stageRequest)).isInstanceOf(WorkflowException.class);

    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof StageFailedEvent stageFailed
                        && stageFailed.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should delegate to underlying executor for actual stage execution")
  void shouldDelegateToUnderlyingExecutorForActualStageExecution() {
    // Arrange
    final var expectedResponse =
        new StageResponse(stageRequest.transactionId(), Collections.emptyMap(), "next-stage");
    when(delegate.execute(stageRequest)).thenReturn(expectedResponse);

    // Act
    final var actualResponse = decorator.execute(stageRequest);

    // Assert
    assertThat(actualResponse).isSameAs(expectedResponse);
    verify(delegate).execute(stageRequest);
  }

  @Test
  @DisplayName("should return delegate key when getKey is called")
  void shouldReturnDelegateKeyWhenGetKeyIsCalled() {
    // Arrange
    when(delegate.getKey()).thenReturn(EXECUTOR_KEY);

    // Act
    final var actualKey = decorator.getKey();

    // Assert
    assertThat(actualKey).isEqualTo(EXECUTOR_KEY);
    verify(delegate).getKey();
  }
}
