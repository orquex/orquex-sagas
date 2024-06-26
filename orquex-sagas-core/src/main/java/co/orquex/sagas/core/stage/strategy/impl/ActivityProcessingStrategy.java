package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.stage.strategy.StrategyResponse;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.transaction.Compensation;
import co.orquex.sagas.domain.utils.Maps;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivityProcessingStrategy extends AbstractStageProcessingStrategy<Activity> {

  public ActivityProcessingStrategy(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository,
      WorkflowEventPublisher eventPublisher) {
    super(taskExecutorRegistry, taskRepository, eventPublisher);
  }

  @Override
  public StrategyResponse process(
      String transactionId, Activity activity, ExecutionRequest request) {
    log.debug("Executing activity stage '{}'", activity.getName());
    // Merge metadata
    final var updatedRequest = request.mergeMetadata(activity.getMetadata());
    // Check if not a parallel execution
    Optional<Map<String, Serializable>> payload;
    // TODO check when all or nothing is true for an activity
    if (!activity.isParallel()) {
      payload =
          activity.getActivityTasks().stream()
              .map(
                  activityTask ->
                      this.processActivityTask(transactionId, activityTask, updatedRequest))
              .filter(Objects::nonNull)
              .filter(m -> !m.isEmpty())
              .reduce(Maps::merge);
    } else {
      final Function<ActivityTask, Supplier<Map<String, Serializable>>> createSubtask =
          activityTask -> () -> processActivityTask(transactionId, activityTask, updatedRequest);
      try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        @SuppressWarnings("unchecked")
        final CompletableFuture<Map<String, Serializable>>[] subtasks =
            activity.getActivityTasks().stream()
                .map(createSubtask)
                .map(supplier -> CompletableFuture.supplyAsync(supplier, executor))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(subtasks);
        payload =
            Arrays.stream(subtasks)
                .map(CompletableFuture::join)
                .filter(m -> m != null && !m.isEmpty())
                .reduce(Maps::merge);
      } catch (CompletionException e) {
        if (e.getCause() instanceof WorkflowException workflowException) {
          throw workflowException;
        }
        throw new WorkflowException(e.getMessage());
      }
    }

    return StrategyResponse.builder()
        .outgoing(activity.getOutgoing())
        .payload(payload.orElse(Collections.emptyMap()))
        .build();
  }

  private Map<String, Serializable> processActivityTask(
      String transactionId, ActivityTask activityTask, ExecutionRequest executionRequest) {
    // Merge payload of the activity task with the current executionRequest
    executionRequest = executionRequest.mergeMetadata(activityTask.metadata());
    // Pre-process the payload with a task
    if (activityTask.preProcessor() != null) {
      var preProcessorPayload =
          executeProcessor(transactionId, activityTask.preProcessor(), executionRequest);
      executionRequest = executionRequest.withPayload(preProcessorPayload);
    }
    // Execute the task with the pre-processed payload
    final var taskResponse = executeTask(transactionId, activityTask.task(), executionRequest);
    // Publish the compensation's event once the task is executed
    executeCompensation(transactionId, activityTask, executionRequest, taskResponse);
    // Post process the payload, generating a new one
    if (activityTask.postProcessor() != null) {
      return executeProcessor(
          transactionId, activityTask.postProcessor(), executionRequest.withPayload(taskResponse));
    }
    return taskResponse;
  }

  private void executeCompensation(
      final String transactionId,
      final ActivityTask activityTask,
      final ExecutionRequest executionRequest,
      final Map<String, Serializable> taskResponse) {
    final var compensationProcessor = activityTask.compensation();
    if (compensationProcessor != null) {
      final var metadata =
          Maps.merge(executionRequest.metadata(), compensationProcessor.metadata());
      eventPublisher.publish(
          new EventMessage<>(
              new Compensation(
                  transactionId,
                  compensationProcessor.task(),
                  metadata,
                  executionRequest.payload(),
                  taskResponse,
                  Instant.now())));
    }
  }
}
