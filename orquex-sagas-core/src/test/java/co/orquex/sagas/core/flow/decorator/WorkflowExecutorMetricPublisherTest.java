package co.orquex.sagas.core.flow.decorator;

import static co.orquex.sagas.core.fixture.ExecutionRequestFixture.getExecutionRequest;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.event.metric.FlowCompletedEvent;
import co.orquex.sagas.domain.event.metric.FlowFailedEvent;
import co.orquex.sagas.domain.event.metric.FlowStartedEvent;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowExecutorMetricPublisher Tests")
class WorkflowExecutorMetricPublisherTest {

  static final String TRANSACTION_ID = "test-transaction-id";

  @Mock WorkflowEventPublisher eventPublisher;

  WorkflowExecutorMetricPublisher publisher;
  ExecutionRequest executionRequest;

  @BeforeEach
  void setUp() {
    publisher = new WorkflowExecutorMetricPublisher(eventPublisher);
    executionRequest = getExecutionRequest();
  }

  @Test
  @DisplayName("should publish FlowStartedEvent with correct data")
  void shouldPublishFlowStartedEventWithCorrectData() {
    // Arrange
    final var timestamp = Instant.now();

    // Act
    publisher.publishFlowStartedEvent(executionRequest, timestamp);

    // Assert
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message()
                            instanceof
                            FlowStartedEvent(
                                String flowId,
                                String correlationId,
                                Instant timestamp1)
                        && flowId.equals(executionRequest.flowId())
                        && correlationId.equals(executionRequest.correlationId())
                        && timestamp1.equals(timestamp)));
  }

  @Test
  @DisplayName("should publish FlowCompletedEvent with correct data")
  void shouldPublishFlowCompletedEventWithCorrectData() {
    // Arrange
    final var executionDuration = Duration.ofMillis(100);

    // Act
    publisher.publishFlowCompletedEvent(executionRequest, TRANSACTION_ID, executionDuration);

    // Assert
    verify(eventPublisher)
        .publish(
            argThat(
                (EventMessage<?> event) ->
                    event.message()
                            instanceof
                            FlowCompletedEvent(
                                String flowId,
                                String correlationId,
                                String transactionId,
                                Instant timestamp,
                                Duration duration)
                        && flowId.equals(executionRequest.flowId())
                        && correlationId.equals(executionRequest.correlationId())
                        && transactionId.equals(TRANSACTION_ID)
                        && duration.equals(executionDuration)
                        && timestamp != null));
  }

  @Test
  @DisplayName("should publish FlowFailedEvent with correct data")
  void shouldPublishFlowFailedEventWithCorrectData() {
    // Arrange
    final var executionDuration = Duration.ofMillis(200);

    // Act
    publisher.publishFlowFailedEvent(executionRequest, executionDuration);

    // Assert
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
                        && duration.equals(executionDuration)
                        && timestamp != null));
  }

  @Test
  @DisplayName("should handle publishing failure gracefully for FlowStartedEvent")
  void shouldHandlePublishingFailureGracefullyForFlowStartedEvent() {
    // Arrange
    final var timestamp = Instant.now();
    doThrow(new RuntimeException("Publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowStartedEvent));

    // Act & Assert
    assertThatCode(() -> publisher.publishFlowStartedEvent(executionRequest, timestamp))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should handle publishing failure gracefully for FlowCompletedEvent")
  void shouldHandlePublishingFailureGracefullyForFlowCompletedEvent() {
    // Arrange
    final var duration = Duration.ofMillis(100);
    doThrow(new RuntimeException("Publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowCompletedEvent));

    // Act & Assert
    assertThatCode(
            () -> publisher.publishFlowCompletedEvent(executionRequest, TRANSACTION_ID, duration))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should handle publishing failure gracefully for FlowFailedEvent")
  void shouldHandlePublishingFailureGracefullyForFlowFailedEvent() {
    // Arrange
    final var duration = Duration.ofMillis(200);
    doThrow(new RuntimeException("Publishing failed"))
        .when(eventPublisher)
        .publish(argThat((EventMessage<?> event) -> event.message() instanceof FlowFailedEvent));

    // Act & Assert
    assertThatCode(() -> publisher.publishFlowFailedEvent(executionRequest, duration))
        .doesNotThrowAnyException();
  }
}
