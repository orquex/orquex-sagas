package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.stage.StageResponse;
import co.orquex.sagas.domain.transaction.Compensation;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.utils.Maps;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for processing activities in a workflow. It extends the
 * AbstractStageProcessingStrategy and overrides the process method.
 */
@Slf4j
public class ActivityProcessingStrategy extends AbstractStageProcessingStrategy<Activity> {

  private final Consumer<Compensation> compensationConsumer;

  public ActivityProcessingStrategy(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository,
      Consumer<Compensation> compensationConsumer) {
    super(taskExecutorRegistry, taskRepository);
    this.compensationConsumer = compensationConsumer;
  }

  /**
   * Processes an activity within a workflow.
   *
   * @param transactionId The ID of the transaction.
   * @param activity The activity to be processed.
   * @param executionRequest The stage execution request.
   * @return The merged response of the executed tasks.
   */
  @Override
  public StageResponse process(
      String transactionId, Activity activity, ExecutionRequest executionRequest) {
    log.debug(
        "Executing activity stage '{}' for flow '{}' and correlation ID '{}'",
        activity.getName(),
        executionRequest.flowId(),
        executionRequest.correlationId());
    // Define the payload to be returned on the StageResponse
    Optional<Map<String, Serializable>> payload;
    // Check if not a parallel execution
    if (!activity.isParallel()) {
      payload = executeSequentially(activity, transactionId, executionRequest);
    } else {
      payload = executeInParallel(activity, transactionId, executionRequest);
    }

    return StageResponse.builder()
        .transactionId(transactionId)
        .outgoing(activity.getOutgoing())
        .payload(payload.orElse(Collections.emptyMap()))
        .build();
  }

  /**
   * Executes the tasks of an activity sequentially.
   *
   * @param activity The activity whose tasks are to be executed.
   * @param transactionId The ID of the transaction.
   * @param updatedRequest The updated execution request.
   * @return The payload resulting from the execution of the tasks.
   */
  private Optional<Map<String, Serializable>> executeSequentially(
      Activity activity, String transactionId, ExecutionRequest updatedRequest) {
    return activity.getActivityTasks().stream()
        .map(
            handleAllOrNothingActivityTaskSequentialExecution(
                activity.isAllOrNothing(), transactionId, updatedRequest))
        .filter(Objects::nonNull)
        .filter(m -> !m.isEmpty())
        .reduce(Maps::merge);
  }

  /**
   * Executes the tasks of an activity in parallel.
   *
   * @param activity The activity whose tasks are to be executed.
   * @param transactionId The ID of the transaction.
   * @param updatedRequest The updated execution request.
   * @return The payload resulting from the execution of the tasks.
   */
  private Optional<Map<String, Serializable>> executeInParallel(
      Activity activity, String transactionId, ExecutionRequest updatedRequest) {
    final Function<ActivityTask, Supplier<Map<String, Serializable>>> createSubtask =
        activityTask -> () -> processActivityTask(transactionId, activityTask, updatedRequest);
    final var factory = Thread.ofVirtual().name("activity-", 0).factory();
    try (final var executor = Executors.newThreadPerTaskExecutor(factory)) {
      @SuppressWarnings("unchecked")
      final CompletableFuture<Map<String, Serializable>>[] subtasks =
          activity.getActivityTasks().stream()
              .map(createSubtask)
              .map(supplier -> CompletableFuture.supplyAsync(supplier, executor))
              .map(handleAllOrNothingActivityTaskParallelExecution(activity.isAllOrNothing()))
              .toArray(CompletableFuture[]::new);
      CompletableFuture.allOf(subtasks);
      return Arrays.stream(subtasks)
          .map(CompletableFuture::join)
          .filter(m -> m != null && !m.isEmpty())
          .reduce(Maps::merge);
    } catch (CompletionException e) {
      throw handleWorkflowException(e);
    }
  }

  /**
   * Handles the execution of an activity task sequentially.
   *
   * @param allOrNothing A flag indicating whether all tasks should be executed or none.
   * @param transactionId The ID of the transaction.
   * @param request The execution request.
   * @return A function that processes an activity task.
   */
  private Function<ActivityTask, Map<String, Serializable>>
      handleAllOrNothingActivityTaskSequentialExecution(
          final boolean allOrNothing, final String transactionId, final ExecutionRequest request) {
    return activityTask -> {
      try {
        return processActivityTask(transactionId, activityTask, request);
      } catch (WorkflowException e) {
        if (allOrNothing) {
          log.debug("Handling activity task sequential execution when all or nothing", e);
          throw e;
        }
        return Collections.emptyMap();
      }
    };
  }

  /**
   * Handles the execution of an activity task parallelly.
   *
   * @param allOrNothing A flag indicating whether all tasks should be executed or none.
   * @return A function that processes a future resulting from the execution of an activity task.
   */
  private static Function<
          CompletableFuture<Map<String, Serializable>>, CompletableFuture<Map<?, ?>>>
      handleAllOrNothingActivityTaskParallelExecution(final boolean allOrNothing) {
    return future ->
        future.handle(
            (result, throwable) -> {
              if (throwable != null) {
                if (allOrNothing) {
                  log.debug(
                      "Handling activity task parallel execution when all or nothing", throwable);
                  throw handleWorkflowException(throwable);
                }
                return Collections.emptyMap();
              }
              return result;
            });
  }

  /**
   * Processes an activity task.
   *
   * @param transactionId The ID of the transaction.
   * @param activityTask The activity task to be processed.
   * @param executionRequest The execution request.
   * @return The payload resulting from the processing of the activity task.
   */
  private Map<String, Serializable> processActivityTask(
      String transactionId, ActivityTask activityTask, ExecutionRequest executionRequest) {
    // Merge metadata of the activity task with the current executionRequest
    executionRequest = executionRequest.mergeMetadata(activityTask.metadata());
    // Pre-process the payload with a task
    final var preProcessor = activityTask.preProcessor();
    if (preProcessor != null) {
      log.debug(
          "Executing pre-processor '{}' for task '{}'", preProcessor.task(), activityTask.task());
      final var preProcessorPayload =
          executeProcessor(transactionId, preProcessor, executionRequest);
      executionRequest = executionRequest.withPayload(preProcessorPayload);
    }
    log.debug(
        "Executing task '{}' at flow '{}' with correlation ID '{}'",
        activityTask.task(),
        executionRequest.flowId(),
        executionRequest.correlationId());
    // Execute the task with the pre-processed payload
    final var taskResponse = executeTask(transactionId, activityTask.task(), executionRequest);
    // Publish the compensation's event once the task is executed
    publishCompensation(transactionId, activityTask, executionRequest, taskResponse);
    // Post process the payload, generating a new one
    final var postProcessor = activityTask.postProcessor();
    if (postProcessor != null) {
      log.debug(
          "Executing post-processor '{}' for task '{}'", postProcessor.task(), activityTask.task());
      return executeProcessor(
          transactionId, postProcessor, executionRequest.withPayload(taskResponse));
    }
    return taskResponse;
  }

  /**
   * Publish the compensation tasks of an executed activity.
   *
   * @param transactionId The ID of the transaction.
   * @param activityTask The activity task for which the compensation is to be executed.
   * @param executionRequest The execution request.
   * @param taskResponse The response of the task.
   */
  private void publishCompensation(
      final String transactionId,
      final ActivityTask activityTask,
      final ExecutionRequest executionRequest,
      final Map<String, Serializable> taskResponse) {
    final var compensationProcessor = activityTask.compensation();
    if (compensationProcessor != null) {
      log.debug("Publishing compensation for task '{}'", activityTask.task());
      final var metadata =
          Maps.merge(executionRequest.metadata(), compensationProcessor.metadata());
      compensationConsumer.accept(
          new Compensation(
              transactionId,
              executionRequest.flowId(),
              executionRequest.correlationId(),
              compensationProcessor.task(),
              metadata,
              executionRequest.payload(),
              taskResponse,
              compensationProcessor.preProcessor(),
              compensationProcessor.postProcessor(),
              Status.CREATED,
              Instant.now()));
    }
  }

  /**
   * Handles a WorkflowException.
   *
   * <p>This method checks if the cause of the provided Throwable is a WorkflowException. If it is,
   * it returns the cause directly. If it is not, it creates a new WorkflowException with the
   * message of the provided Throwable and returns it.
   *
   * @param throwable The Throwable to be handled.
   * @return A WorkflowException derived from the provided Throwable.
   */
  private static WorkflowException handleWorkflowException(Throwable throwable) {
    if (throwable.getCause() instanceof WorkflowException workflowException) {
      return workflowException;
    }
    return new WorkflowException(throwable.getMessage());
  }
}
