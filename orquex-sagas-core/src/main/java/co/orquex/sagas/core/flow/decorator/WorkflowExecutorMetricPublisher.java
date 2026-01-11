package co.orquex.sagas.core.flow.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.event.metric.FlowCompletedEvent;
import co.orquex.sagas.domain.event.metric.FlowFailedEvent;
import co.orquex.sagas.domain.event.metric.FlowStartedEvent;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class responsible for publishing workflow execution metric events.
 *
 * <p>This class encapsulates the logic for publishing flow lifecycle events during workflow
 * execution, providing a clean separation of concerns for metrics collection. It publishes events
 * through the configured {@link WorkflowEventPublisher} and handles publishing failures gracefully
 * by logging warnings without interrupting the workflow execution.
 *
 * <p>The class publishes the following events:
 *
 * <ul>
 *   <li>{@link FlowStartedEvent} - Indicates the start of workflow execution
 *   <li>{@link FlowCompletedEvent} - Indicates successful completion of workflow execution
 *   <li>{@link FlowFailedEvent} - Indicates failure of workflow execution
 * </ul>
 *
 * <p>All events include relevant metadata such as flow ID, correlation ID, timestamps, and
 * execution durations where applicable.
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe assuming the underlying event
 * publisher implementation is thread-safe.
 *
 * @see WorkflowEventPublisher
 * @see FlowStartedEvent
 * @see FlowCompletedEvent
 * @see FlowFailedEvent
 * @see WorkflowExecutorMetricDecorator
 */
public record WorkflowExecutorMetricPublisher(WorkflowEventPublisher eventPublisher) {

  private static final Logger log = LoggerFactory.getLogger(WorkflowExecutorMetricPublisher.class);

    /**
   * Publishes a {@link FlowStartedEvent} to indicate the beginning of workflow execution.
   *
   * <p>This method creates and publishes a flow-started event with the provided timestamp,
   * including the flow ID and correlation ID from the execution request. If publishing fails,
   * a warning is logged, but no exception is thrown to avoid interrupting workflow execution.
   *
   * @param request the execution request containing flow metadata
   * @param timestamp the instant when the workflow execution started
   */
  public void publishFlowStartedEvent(final ExecutionRequest request, final Instant timestamp) {
    try {
      final var event = new FlowStartedEvent(request.flowId(), request.correlationId(), timestamp);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published FlowEventStarted for flowId: {}, correlationId: {}",
          request.flowId(),
          request.correlationId());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish FlowEventStarted for flowId: {}, correlationId: {}",
          request.flowId(),
          request.correlationId(),
          exception);
    }
  }

  /**
   * Publishes a {@link FlowCompletedEvent} to indicate successful completion of workflow execution.
   *
   * <p>This method creates and publishes a flow-completed event with the current timestamp,
   * including the flow ID, correlation ID, transaction ID, and execution duration from the
   * execution request. If publishing fails, a warning is logged, but no exception is thrown
   * to avoid interrupting workflow execution.
   *
   * @param request the execution request containing flow metadata
   * @param transactionId the unique identifier of the completed transaction
   * @param duration the total duration of the workflow execution
   */
  public void publishFlowCompletedEvent(
      final ExecutionRequest request, final String transactionId, final Duration duration) {
    try {
      final var event =
          new FlowCompletedEvent(
              request.flowId(), request.correlationId(), transactionId, Instant.now(), duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published FlowEventCompleted for flowId: {}, correlationId: {}, transactionId: {}, duration: {}ms",
          request.flowId(),
          request.correlationId(),
          transactionId,
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish FlowEventCompleted for flowId: {}, correlationId: {}",
          request.flowId(),
          request.correlationId(),
          exception);
    }
  }

  /**
   * Publishes a {@link FlowFailedEvent} to indicate failure of workflow execution.
   *
   * <p>This method creates and publishes a flow-failed event with the current timestamp,
   * including the flow ID, correlation ID, and execution duration from the execution request.
   * If publishing fails, a warning is logged, but no exception is thrown to avoid interrupting
   * workflow execution.
   *
   * @param request the execution request containing flow metadata
   * @param duration the duration of the workflow execution before failure
   */
  public void publishFlowFailedEvent(final ExecutionRequest request, final Duration duration) {
    try {
      final var event =
          new FlowFailedEvent(request.flowId(), request.correlationId(), Instant.now(), duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published FlowEventFailed for flowId: {}, correlationId: {}, duration: {}ms",
          request.flowId(),
          request.correlationId(),
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish FlowEventFailed for flowId: {}, correlationId: {}",
          request.flowId(),
          request.correlationId(),
          exception);
    }
  }
}
