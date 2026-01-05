package co.orquex.sagas.core.flow.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.event.metric.FlowCompletedEvent;
import co.orquex.sagas.domain.event.metric.FlowFailedEvent;
import co.orquex.sagas.domain.event.metric.FlowStartedEvent;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.execution.ExecutionResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A decorator implementation that provides metrics collection capabilities for workflow execution.
 *
 * <p>This decorator implements the Decorator pattern to wrap any {@link Executable} implementation
 * and transparently adds metrics collection functionality by publishing flow lifecycle events. The
 * decorator maintains complete separation of concerns by keeping metrics logic isolated from the
 * core workflow execution logic.
 *
 * <p>The decorator publishes the following events during workflow execution:
 *
 * <ul>
 *   <li>{@link FlowStartedEvent} - Published at the beginning of workflow execution
 *   <li>{@link FlowCompletedEvent} - Published upon successful workflow completion
 *   <li>{@link FlowFailedEvent} - Published when workflow execution fails with an exception
 * </ul>
 *
 * <p>All events are published through the configured {@link WorkflowEventPublisher}, allowing
 * integration with various monitoring and metrics collection systems without coupling to specific
 * implementations.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * Executable<ExecutionRequest, ExecutionResponse> coreExecutor = new WorkflowExecutor(...);
 * Executable<ExecutionRequest, ExecutionResponse> metricsExecutor =
 *     new WorkflowExecutorMetricDecorator(coreExecutor, eventPublisher);
 *
 * ExecutionResponse response = metricsExecutor.execute(request);
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This decorator is thread-safe assuming the underlying delegate
 * and event publisher implementations are thread-safe.
 *
 * @see Executable
 * @see WorkflowEventPublisher
 * @see FlowStartedEvent
 * @see FlowCompletedEvent
 * @see FlowFailedEvent
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowExecutorMetricDecorator
    implements Executable<ExecutionRequest, ExecutionResponse> {

  private final Executable<ExecutionRequest, ExecutionResponse> delegate;
  private final WorkflowEventPublisher eventPublisher;

  @Override
  public ExecutionResponse execute(final ExecutionRequest request) {
    final var startTime = Instant.now();

    // Publish FlowEventStarted (before transaction creation)
    publishFlowStartedEvent(request, startTime);

    try {
      // Execute the actual workflow
      final var response = delegate.execute(request);

      // Calculate execution duration and publish FlowEventCompleted
      final var executionDuration = Duration.between(startTime, Instant.now());
      publishFlowCompletedEvent(request, response.transactionId(), executionDuration);

      return response;

    } catch (final Exception exception) {
      // Calculate execution duration and publish FlowEventFailed
      final var executionDuration = Duration.between(startTime, Instant.now());
      publishFlowFailedEvent(request, executionDuration);

      // Re-throw the exception to maintain original behavior
      throw exception;
    }
  }

  private void publishFlowStartedEvent(final ExecutionRequest request, final Instant timestamp) {
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

  private void publishFlowCompletedEvent(
      final ExecutionRequest request, final String transactionId, final Duration duration) {
    try {
      final var event =
          new FlowCompletedEvent(
              request.flowId(), request.correlationId(), Instant.now(), duration);
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

  private void publishFlowFailedEvent(final ExecutionRequest request, final Duration duration) {
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
