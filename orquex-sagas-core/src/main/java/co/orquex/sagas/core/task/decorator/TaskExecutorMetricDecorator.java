package co.orquex.sagas.core.task.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.event.metric.TaskCompletedEvent;
import co.orquex.sagas.domain.event.metric.TaskFailedEvent;
import co.orquex.sagas.domain.event.metric.TaskStartedEvent;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * A decorator implementation that provides metrics collection capabilities for task execution.
 *
 * <p>This decorator implements the Decorator pattern to wrap any {@link TaskExecutor}
 * implementation and transparently adds metrics collection functionality by publishing task
 * lifecycle events. The decorator maintains complete separation of concerns by keeping metrics
 * logic isolated from the core task execution logic.
 *
 * <p>The decorator publishes the following events during task execution:
 *
 * <ul>
 *   <li>{@link TaskStartedEvent} - Published at the beginning of task execution
 *   <li>{@link TaskCompletedEvent} - Published upon successful task completion
 *   <li>{@link TaskFailedEvent} - Published when task execution fails with an exception
 * </ul>
 *
 * <p>All events are published through the configured {@link WorkflowEventPublisher}, allowing
 * integration with various monitoring and metrics collection systems without coupling to specific
 * implementations.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * TaskExecutor coreExecutor = new DefaultTaskExecutor(...);
 * TaskExecutor metricsExecutor =
 *     new TaskExecutorMetricDecorator(coreExecutor, eventPublisher);
 *
 * TaskResponse response = metricsExecutor.execute(taskRequest);
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This decorator is thread-safe assuming the underlying delegate
 * and event publisher implementations are thread-safe.
 *
 * @since 1.0.0
 * @see TaskExecutor
 * @see WorkflowEventPublisher
 * @see TaskStartedEvent
 * @see TaskCompletedEvent
 * @see TaskFailedEvent
 */
@Slf4j
public class TaskExecutorMetricDecorator implements TaskExecutor {

  private final TaskExecutor delegate;
  private final WorkflowEventPublisher eventPublisher;

  public TaskExecutorMetricDecorator(TaskExecutor delegate, WorkflowEventPublisher eventPublisher) {
    this.delegate = delegate;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Task task, ExecutionRequest request) {
    final var startTime = Instant.now();

    publishTaskStartedEvent(transactionId, task, request, startTime);

    try {
      final var response = delegate.execute(transactionId, task, request);

      final var executionDuration = Duration.between(startTime, Instant.now());
      publishTaskCompletedEvent(transactionId, task, request, executionDuration);

      return response;

    } catch (final Exception exception) {
      final var executionDuration = Duration.between(startTime, Instant.now());
      publishTaskFailedEvent(transactionId, task, request, executionDuration);

      throw exception;
    }
  }

  @Override
  public String getKey() {
    return delegate.getKey();
  }

  private void publishTaskStartedEvent(
      final String transactionId,
      final Task task,
      final ExecutionRequest request,
      final Instant timestamp) {
    try {
      final var event =
          new TaskStartedEvent(
              request.flowId(), request.correlationId(), transactionId, task.name(), timestamp);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published TaskStartedEvent for flowId: {}, correlationId: {}, task: {}",
          request.flowId(),
          request.correlationId(),
          task.name());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish TaskStartedEvent for flowId: {}, correlationId: {}, task: {}",
          request.flowId(),
          request.correlationId(),
          task.name(),
          exception);
    }
  }

  private void publishTaskCompletedEvent(
      final String transactionId,
      final Task task,
      final ExecutionRequest request,
      final Duration duration) {
    try {
      final var event =
          new TaskCompletedEvent(
              request.flowId(),
              request.correlationId(),
              transactionId,
              task.name(),
              Instant.now(),
              duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published TaskCompletedEvent for flowId: {}, correlationId: {}, task: {}, duration: {}ms",
          request.flowId(),
          request.correlationId(),
          task.name(),
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish TaskCompletedEvent for flowId: {}, correlationId: {}, task: {}",
          request.flowId(),
          request.correlationId(),
          task.name(),
          exception);
    }
  }

  private void publishTaskFailedEvent(
      String transactionId, Task task, ExecutionRequest request, final Duration duration) {
    try {
      final var event =
          new TaskFailedEvent(
              request.flowId(),
              request.correlationId(),
              transactionId,
              task.name(),
              Instant.now(),
              duration);
      eventPublisher.publish(new EventMessage<>(event));
      log.debug(
          "Published TaskFailedEvent for flowId: {}, correlationId: {}, task: {}, duration: {}ms",
          request.flowId(),
          request.correlationId(),
          task.name(),
          duration.toMillis());
    } catch (final Exception exception) {
      log.warn(
          "Failed to publish TaskFailedEvent for flowId: {}, correlationId: {}, task: {}",
          request.flowId(),
          request.correlationId(),
          task.name(),
          exception);
    }
  }
}
