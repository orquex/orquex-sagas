package co.orquex.sagas.core.task.decorator;

import static co.orquex.sagas.core.fixture.ExecutionRequestFixture.getExecutionRequest;
import static co.orquex.sagas.core.fixture.TaskFixture.getTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.event.metric.TaskCompletedEvent;
import co.orquex.sagas.domain.event.metric.TaskFailedEvent;
import co.orquex.sagas.domain.event.metric.TaskStartedEvent;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskExecutorMetricDecorator Tests")
class TaskExecutorMetricDecoratorTest {

  private static final String TRANSACTION_ID = "test-transaction-id";
  private static final String TASK_ID = "test-task";
  private static final String EXECUTOR_KEY = "test-executor";

  @Mock private TaskExecutor delegate;
  @Mock private WorkflowEventPublisher eventPublisher;

  private TaskExecutorMetricDecorator decorator;
  private ExecutionRequest executionRequest;
  private Task task;

  @BeforeEach
  void setUp() {
    decorator = new TaskExecutorMetricDecorator(delegate, eventPublisher);
    executionRequest = getExecutionRequest();
    task = getTask(TASK_ID);
  }

  @Test
  @DisplayName("should execute task successfully and publish started and completed events")
  void shouldExecuteTaskSuccessfullyAndPublishEvents() {
    // Arrange
    final Map<String, Serializable> expectedResult = Collections.emptyMap();
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenReturn(expectedResult);

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult).isNotNull().isEqualTo(expectedResult);

    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskStartedEvent taskStarted
                        && taskStarted.flowId().equals(executionRequest.flowId())
                        && taskStarted.correlationId().equals(executionRequest.correlationId())
                        && taskStarted.transactionId().equals(TRANSACTION_ID)
                        && taskStarted.task().equals(TASK_ID)
                        && taskStarted.timestamp() != null));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskCompletedEvent taskCompleted
                        && taskCompleted.flowId().equals(executionRequest.flowId())
                        && taskCompleted.correlationId().equals(executionRequest.correlationId())
                        && taskCompleted.transactionId().equals(TRANSACTION_ID)
                        && taskCompleted.task().equals(TASK_ID)
                        && taskCompleted.timestamp() != null
                        && taskCompleted.duration() != null));
  }

  @Test
  @DisplayName("should publish failed event when task execution throws exception")
  void shouldPublishFailedEventWhenTaskExecutionThrowsException() {
    // Arrange
    final var exception = new WorkflowException("Task execution failed");
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(TRANSACTION_ID, task, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task execution failed");

    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskStartedEvent taskStarted
                        && taskStarted.flowId().equals(executionRequest.flowId())
                        && taskStarted.correlationId().equals(executionRequest.correlationId())
                        && taskStarted.transactionId().equals(TRANSACTION_ID)
                        && taskStarted.task().equals(TASK_ID)));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskFailedEvent taskFailed
                        && taskFailed.flowId().equals(executionRequest.flowId())
                        && taskFailed.correlationId().equals(executionRequest.correlationId())
                        && taskFailed.transactionId().equals(TRANSACTION_ID)
                        && taskFailed.task().equals(TASK_ID)
                        && taskFailed.timestamp() != null
                        && taskFailed.duration() != null));
    verify(eventPublisher, never())
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskCompletedEvent));
  }

  @Test
  @DisplayName("should continue task execution even if started event publishing fails")
  void shouldContinueTaskExecutionEvenIfStartedEventPublishingFails() {
    // Arrange
    final Map<String, Serializable> expectedResult = Collections.emptyMap();
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenReturn(expectedResult);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskStartedEvent));

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult).isNotNull().isEqualTo(expectedResult);
    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskCompletedEvent));
  }

  @Test
  @DisplayName("should continue task execution even if completed event publishing fails")
  void shouldContinueTaskExecutionEvenIfCompletedEventPublishingFails() {
    // Arrange
    final Map<String, Serializable> expectedResult = Collections.emptyMap();
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenReturn(expectedResult);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskCompletedEvent));

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult).isNotNull().isEqualTo(expectedResult);
    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
  }

  @Test
  @DisplayName("should rethrow exception even if failed event publishing fails")
  void shouldRethrowExceptionEvenIfFailedEventPublishingFails() {
    // Arrange
    final var exception = new WorkflowException("Task execution failed");
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenThrow(exception);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskFailedEvent));

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(TRANSACTION_ID, task, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task execution failed");

    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
  }

  @Test
  @DisplayName("should measure execution duration correctly for successful execution")
  void shouldMeasureExecutionDurationCorrectlyForSuccessfulExecution() {
    // Arrange
    final Map<String, Serializable> expectedResult = Collections.emptyMap();
    when(delegate.execute(TRANSACTION_ID, task, executionRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              return expectedResult;
            });

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult).isNotNull();
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskCompletedEvent taskCompleted
                        && taskCompleted.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should measure execution duration correctly for failed execution")
  void shouldMeasureExecutionDurationCorrectlyForFailedExecution() {
    // Arrange
    when(delegate.execute(TRANSACTION_ID, task, executionRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              throw new WorkflowException("Task execution failed");
            });

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(TRANSACTION_ID, task, executionRequest))
        .isInstanceOf(WorkflowException.class);

    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof TaskFailedEvent taskFailed
                        && taskFailed.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should delegate to underlying executor for actual task execution")
  void shouldDelegateToUnderlyingExecutorForActualTaskExecution() {
    // Arrange
    final Map<String, Serializable> expectedResult = Collections.emptyMap();
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenReturn(expectedResult);

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult).isSameAs(expectedResult);
    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
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

  @Test
  @DisplayName("should handle task execution with non-empty result payload")
  void shouldHandleTaskExecutionWithNonEmptyResultPayload() {
    // Arrange
    final Map<String, Serializable> expectedResult = Map.of("key1", "value1", "key2", 123);
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenReturn(expectedResult);

    // Act
    final var actualResult = decorator.execute(TRANSACTION_ID, task, executionRequest);

    // Assert
    assertThat(actualResult)
        .isNotNull()
        .hasSize(2)
        .containsEntry("key1", "value1")
        .containsEntry("key2", 123);
    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskStartedEvent));
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskCompletedEvent));
  }

  @Test
  @DisplayName("should handle runtime exception during task execution")
  void shouldHandleRuntimeExceptionDuringTaskExecution() {
    // Arrange
    final var exception = new RuntimeException("Unexpected runtime error");
    when(delegate.execute(TRANSACTION_ID, task, executionRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(TRANSACTION_ID, task, executionRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Unexpected runtime error");

    verify(delegate).execute(TRANSACTION_ID, task, executionRequest);
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskStartedEvent));
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof TaskFailedEvent));
  }
}
