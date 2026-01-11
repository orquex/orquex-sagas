package co.orquex.sagas.core.stage.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.event.metric.StageCompletedEvent;
import co.orquex.sagas.domain.event.metric.StageFailedEvent;
import co.orquex.sagas.domain.event.metric.StageStartedEvent;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.stage.StageResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A decorator implementation that provides metrics collection capabilities for stage execution.
 *
 * <p>This decorator implements the Decorator pattern to wrap any {@link StageExecutor}
 * implementation and transparently adds metrics collection functionality by publishing stage
 * lifecycle events. The decorator maintains complete separation of concerns by keeping metrics
 * logic isolated from the core stage execution logic.
 *
 * <p>The decorator publishes the following events during stage execution:
 *
 * <ul>
 *   <li>{@link StageStartedEvent} - Published at the beginning of stage execution
 *   <li>{@link StageCompletedEvent} - Published upon successful stage completion
 *   <li>{@link StageFailedEvent} - Published when stage execution fails with an exception
 * </ul>
 *
 * <p>All events are published through the configured {@link WorkflowEventPublisher}, allowing
 * integration with various monitoring and metrics collection systems without coupling to specific
 * implementations.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * StageExecutor coreExecutor = new DefaultStageExecutor(...);
 * StageExecutor metricsExecutor =
 *     new StageExecutorMetricDecorator(coreExecutor, eventPublisher);
 *
 * StageResponse response = metricsExecutor.execute(stageRequest);
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This decorator is thread-safe assuming the underlying delegate
 * and event publisher implementations are thread-safe.
 *
 * @since 1.0.0
 * @see StageExecutor
 * @see WorkflowEventPublisher
 * @see StageStartedEvent
 * @see StageCompletedEvent
 * @see StageFailedEvent
 */
@Slf4j
@RequiredArgsConstructor
public class StageExecutorMetricDecorator implements StageExecutor {

  private final StageExecutor delegate;
  private final WorkflowEventPublisher eventPublisher;

  @Override
  public StageResponse execute(final StageRequest request) {
    final var startTime = Instant.now();

    // Publish StageStartedEvent
    publishStageStartedEvent(request, startTime);

    try {
      // Execute the actual stage
      final var response = delegate.execute(request);

      // Calculate execution duration and publish StageCompletedEvent
      final var executionDuration = Duration.between(startTime, Instant.now());
      publishStageCompletedEvent(request, executionDuration);

      return response;

    } catch (final Exception exception) {
      // Calculate execution duration and publish StageFailedEvent
      final var executionDuration = Duration.between(startTime, Instant.now());
      publishStageFailedEvent(request, executionDuration);

      // Re-throw the exception to maintain original behavior
      throw exception;
    }
  }

  @Override
  public String getKey() {
    return delegate.getKey();
  }

  private void publishStageStartedEvent(final StageRequest request, final Instant timestamp) {
    try {
      final var event =
          new StageStartedEvent(
              request.executionRequest().flowId(),
              request.executionRequest().correlationId(),
              request.transactionId(),
              request.stage().getName(),
              timestamp);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published StageStartedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish StageStartedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId(),
          exception);
    }
  }

  private void publishStageCompletedEvent(final StageRequest request, final Duration duration) {
    try {
      final var event =
          new StageCompletedEvent(
              request.executionRequest().flowId(),
              request.executionRequest().correlationId(),
              request.transactionId(),
              request.stage().getName(),
              Instant.now(),
              duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published StageCompletedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}, duration: {}ms",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId(),
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish StageCompletedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId(),
          exception);
    }
  }

  private void publishStageFailedEvent(final StageRequest request, final Duration duration) {
    try {
      final var event =
          new StageFailedEvent(
              request.executionRequest().flowId(),
              request.executionRequest().correlationId(),
              request.transactionId(),
              request.stage().getName(),
              Instant.now(),
              duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published StageFailedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}, duration: {}ms",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId(),
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish StageFailedEvent for flowId: {}, correlationId: {}, transactionId: {}, stage: {}",
          request.executionRequest().flowId(),
          request.executionRequest().correlationId(),
          request.transactionId(),
          request.stage().getId(),
          exception);
    }
  }
}
