package co.orquex.sagas.core.flow.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.event.metric.FlowCompletedEvent;
import co.orquex.sagas.domain.event.metric.FlowFailedEvent;
import co.orquex.sagas.domain.event.metric.FlowStartedEvent;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.execution.ExecutionResponse;
import java.time.Duration;
import java.time.Instant;

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
public class WorkflowExecutorMetricDecorator
    implements Executable<ExecutionRequest, ExecutionResponse> {

  private final Executable<ExecutionRequest, ExecutionResponse> delegate;
  private final WorkflowExecutorMetricPublisher executorMetricPublisher;

  public WorkflowExecutorMetricDecorator(
      final Executable<ExecutionRequest, ExecutionResponse> delegate,
      final WorkflowEventPublisher eventPublisher) {
    this.delegate = delegate;
    this.executorMetricPublisher = new WorkflowExecutorMetricPublisher(eventPublisher);
  }

  @Override
  public ExecutionResponse execute(final ExecutionRequest request) {
    final var startTime = Instant.now();

    // Publish FlowEventStarted (before transaction creation)
    executorMetricPublisher.publishFlowStartedEvent(request, startTime);

    try {
      // Execute the actual workflow
      final var response = delegate.execute(request);

      // Calculate execution duration and publish FlowEventCompleted
      final var executionDuration = Duration.between(startTime, Instant.now());
      executorMetricPublisher.publishFlowCompletedEvent(
          request, response.transactionId(), executionDuration);

      return response;

    } catch (final Exception exception) {
      // Calculate execution duration and publish FlowEventFailed
      final var executionDuration = Duration.between(startTime, Instant.now());
      executorMetricPublisher.publishFlowFailedEvent(request, executionDuration);

      // Re-throw the exception to maintain original behavior
      throw exception;
    }
  }
}
