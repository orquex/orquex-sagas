package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.core.stage.strategy.StrategyResponse;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.utils.Maps;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class ActivityProcessingStrategy extends AbstractStageProcessingStrategy<Activity> {

  public ActivityProcessingStrategy(
      Registry<TaskExecutor> taskExecutorRegistry, TaskRepository taskRepository) {
    super(taskExecutorRegistry, taskRepository);
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
              .map(activityTask -> this.processActivityTask(transactionId, activityTask, updatedRequest))
              .filter(m -> m != null && !m.isEmpty())
              .reduce(Maps::merge);
    } else {
      final Function<ActivityTask, Supplier<Map<String, Serializable>>> createSubtask =
              (activityTask) -> () -> processActivityTask(transactionId, activityTask, updatedRequest);
      try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        @SuppressWarnings("unchecked")
        final CompletableFuture<Map<String, Serializable>>[] subtasks =
                activity.getActivityTasks().stream()
                        .map(createSubtask)
                        .map(supplier -> CompletableFuture.supplyAsync(supplier, executor))
                        .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(subtasks);
        payload = Arrays.stream(subtasks)
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
      String transactionId, ActivityTask activityTask, ExecutionRequest request) {
    // Merge payload of the activity task with the current executionRequest
    request = request.mergeMetadata(activityTask.metadata());
    // Pre-process the payload with a task
    if (activityTask.preProcessor() != null) {
      var preProcessorPayload =
          executeProcessor(transactionId, activityTask.preProcessor(), request);
      request = request.withPayload(preProcessorPayload);
    }
    // Execute the task with the pre-processed payload
    var taskResponse = executeTask(transactionId, activityTask.task(), request);
    // TODO Publish the event of Compensations once the task is executed
    // Post process the payload, generating a new one
    if (activityTask.postProcessor() != null) {
      return executeProcessor(
          transactionId, activityTask.postProcessor(), request.withPayload(taskResponse));
    }
    return taskResponse;
  }
}
