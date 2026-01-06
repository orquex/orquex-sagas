package co.orquex.sagas.core.flow.decorator;

import static co.orquex.sagas.core.fixture.ExecutionRequestFixture.getExecutionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.event.metric.FlowCompletedEvent;
import co.orquex.sagas.domain.event.metric.FlowFailedEvent;
import co.orquex.sagas.domain.event.metric.FlowStartedEvent;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.execution.ExecutionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowExecutorMetricDecorator Tests")
class WorkflowExecutorMetricDecoratorTest {

  static final String TRANSACTION_ID = "test-transaction-id";

  @Mock Executable<ExecutionRequest, ExecutionResponse> delegate;
  @Mock WorkflowEventPublisher eventPublisher;

  WorkflowExecutorMetricDecorator decorator;
  ExecutionRequest executionRequest;

  @BeforeEach
  void setUp() {
    decorator = new WorkflowExecutorMetricDecorator(delegate, eventPublisher);
    executionRequest = getExecutionRequest();
  }

  @Test
  @DisplayName("should execute workflow successfully and publish started and completed events")
  void shouldExecuteWorkflowSuccessfullyAndPublishEvents() {
    // Arrange
    final var expectedResponse = new ExecutionResponse(TRANSACTION_ID, Collections.emptyMap());
    when(delegate.execute(executionRequest)).thenReturn(expectedResponse);

    // Act
    final var actualResponse = decorator.execute(executionRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(TRANSACTION_ID, ExecutionResponse::transactionId)
        .returns(Collections.emptyMap(), ExecutionResponse::payload);

    verify(delegate).execute(executionRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message()
                            instanceof
                            FlowStartedEvent(String flowId, String correlationId, Instant timestamp)
                        && flowId.equals(executionRequest.flowId())
                        && correlationId.equals(executionRequest.correlationId())
                        && timestamp != null));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof FlowCompletedEvent flowCompleted
                        && flowCompleted.flowId().equals(executionRequest.flowId())
                        && flowCompleted.correlationId().equals(executionRequest.correlationId())
                        && flowCompleted.timestamp() != null
                        && flowCompleted.duration() != null));
  }

  @Test
  @DisplayName("should publish failed event when workflow execution throws exception")
  void shouldPublishFailedEventWhenWorkflowExecutionThrowsException() {
    // Arrange
    final var exception = new WorkflowException("Workflow execution failed");
    when(delegate.execute(executionRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Workflow execution failed");

    verify(delegate).execute(executionRequest);
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof FlowStartedEvent flowStarted
                        && flowStarted.flowId().equals(executionRequest.flowId())
                        && flowStarted.correlationId().equals(executionRequest.correlationId())));
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message()
                            instanceof
                            FlowFailedEvent(
                                String flowId,
                                String correlationId,
                                Instant timestamp,
                                Duration duration)
                        && flowId.equals(executionRequest.flowId())
                        && correlationId.equals(executionRequest.correlationId())
                        && timestamp != null
                        && duration != null));
    verify(eventPublisher, never())
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowCompletedEvent));
  }

  @Test
  @DisplayName("should continue workflow execution even if started event publishing fails")
  void shouldContinueWorkflowExecutionEvenIfStartedEventPublishingFails() {
    // Arrange
    final var expectedResponse = new ExecutionResponse(TRANSACTION_ID, Collections.emptyMap());
    when(delegate.execute(executionRequest)).thenReturn(expectedResponse);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowStartedEvent));

    // Act
    final var actualResponse = decorator.execute(executionRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(TRANSACTION_ID, ExecutionResponse::transactionId);
    verify(delegate).execute(executionRequest);
    verify(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowCompletedEvent));
  }

  @Test
  @DisplayName("should continue workflow execution even if completed event publishing fails")
  void shouldContinueWorkflowExecutionEvenIfCompletedEventPublishingFails() {
    // Arrange
    final var expectedResponse = new ExecutionResponse(TRANSACTION_ID, Collections.emptyMap());
    when(delegate.execute(executionRequest)).thenReturn(expectedResponse);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowCompletedEvent));

    // Act
    final var actualResponse = decorator.execute(executionRequest);

    // Assert
    assertThat(actualResponse)
        .isNotNull()
        .returns(TRANSACTION_ID, ExecutionResponse::transactionId);
    verify(delegate).execute(executionRequest);
  }

  @Test
  @DisplayName(
      "should publish failed event and rethrow exception even if failed event publishing fails")
  void shouldRethrowExceptionEvenIfFailedEventPublishingFails() {
    // Arrange
    final var exception = new WorkflowException("Workflow execution failed");
    when(delegate.execute(executionRequest)).thenThrow(exception);
    doThrow(new RuntimeException("Event publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowFailedEvent));

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Workflow execution failed");

    verify(delegate).execute(executionRequest);
  }

  @Test
  @DisplayName("should measure execution duration correctly for successful execution")
  void shouldMeasureExecutionDurationCorrectlyForSuccessfulExecution() {
    // Arrange
    final var expectedResponse = new ExecutionResponse(TRANSACTION_ID, Collections.emptyMap());
    when(delegate.execute(executionRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              return expectedResponse;
            });

    // Act
    final var actualResponse = decorator.execute(executionRequest);

    // Assert
    assertThat(actualResponse).isNotNull();
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof FlowCompletedEvent flowCompleted
                        && flowCompleted.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should measure execution duration correctly for failed execution")
  void shouldMeasureExecutionDurationCorrectlyForFailedExecution() {
    // Arrange
    when(delegate.execute(executionRequest))
        .thenAnswer(
            invocation -> {
              Thread.sleep(50); // Simulate some work
              throw new WorkflowException("Workflow execution failed");
            });

    // Act & Assert
    assertThatThrownBy(() -> decorator.execute(executionRequest))
        .isInstanceOf(WorkflowException.class);

    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message() instanceof FlowFailedEvent flowFailed
                        && flowFailed.duration().toMillis() >= 50));
  }

  @Test
  @DisplayName("should delegate to underlying executor for actual workflow execution")
  void shouldDelegateToUnderlyingExecutorForActualWorkflowExecution() {
    // Arrange
    final var expectedResponse = new ExecutionResponse(TRANSACTION_ID, Collections.emptyMap());
    when(delegate.execute(any(ExecutionRequest.class))).thenReturn(expectedResponse);

    // Act
    final var actualResponse = decorator.execute(executionRequest);

    // Assert
    assertThat(actualResponse).isSameAs(expectedResponse);
    verify(delegate).execute(executionRequest);
  }
}
