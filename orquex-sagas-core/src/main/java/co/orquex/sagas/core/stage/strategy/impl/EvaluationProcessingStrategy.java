package co.orquex.sagas.core.stage.strategy.impl;

import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Condition;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.EvaluationTask;
import co.orquex.sagas.domain.stage.StageResponse;
import java.io.Serializable;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This evaluation processing strategy treats the expression stored in the metadata field {@code
 * __expression} as the condition. It then retrieves the boolean result from the variable {@code
 * __result}. If the retrieved value is either false or not a boolean type at all, the condition is
 * considered not to match.
 */
@Slf4j
public class EvaluationProcessingStrategy extends AbstractStageProcessingStrategy<Evaluation> {

  public static final String EXPRESSION = "__expression";
  public static final String RESULT = "__result";

  /**
   * Constructor for the EvaluationProcessingStrategy.
   *
   * @param taskExecutorRegistry Registry of task executors.
   * @param taskRepository Repository for tasks.
   */
  public EvaluationProcessingStrategy(
      Registry<TaskExecutor> taskExecutorRegistry,
      TaskRepository taskRepository) {
    super(taskExecutorRegistry, taskRepository);
  }

  /**
   * Processes the evaluation stage of a workflow.
   *
   * <p>The method begins by logging the name of the evaluation stage being processed and setting
   * the default outgoing stage.
   *
   * <pre>
   * It then iterates over each condition in the evaluation stage. For each condition:
   * - It merges the metadata of the evaluation stage with the execution executionRequest.
   * - It adds the condition's expression to the metadata of the execution executionRequest.
   * - It calls the `processEvaluationTask` method to execute the evaluation task associated with the current condition.
   * - It checks if the `RESULT` key exists in the response map and if its value is a Boolean. If the value is `true`, it means the condition is met. In this case, the method sets the outgoing stage to the one specified in the current condition and breaks the loop.
   * </pre>
   *
   * If none of the conditions are met (in other words, none of the conditions result in a `true`
   * value), the method returns the default outgoing stage specified in the evaluation.
   *
   * <p>The method finally returns a `StageResponse` object, which contains the outgoing stage and
   * the payload from the execution executionRequest.
   *
   * @param transactionId The ID of the transaction.
   * @param evaluation The evaluation stage to be processed.
   * @param executionRequest The execution executionRequest.
   * @return The response of the strategy, containing the outgoing stage and payload.
   */
  @Override
  public StageResponse process(
      String transactionId, Evaluation evaluation, ExecutionRequest executionRequest) {
    log.debug(
        "Executing evaluation stage '{}' of flow '{}' with correlation ID '{}'",
        evaluation.getName(),
        executionRequest.flowId(),
        executionRequest.correlationId());
    var outgoing = evaluation.getDefaultOutgoing();

    for (Condition condition : evaluation.getConditions()) {
      // add the condition expression to the metadata
      executionRequest = executionRequest.mergeMetadata(evaluation.getMetadata());
      executionRequest.metadata().put(EXPRESSION, condition.expression());
      // Call task
      var response = processEvaluationTask(transactionId, evaluation.getEvaluationTask(), executionRequest);
      // get the evaluation result
      if (response.containsKey(RESULT)
          && response.get(RESULT) instanceof Boolean result
          && Boolean.TRUE.equals(result)) {
        outgoing = condition.outgoing();
        break;
      }
    }

    // return the default outgoing.
    return StageResponse.builder()
        .transactionId(transactionId)
        .outgoing(outgoing)
        .payload(executionRequest.payload())
        .build();
  }

  /**
   * Processes the evaluation task of a workflow.
   *
   * @param transactionId The ID of the transaction.
   * @param evaluationTask The evaluation task to be processed.
   * @param request The execution request.
   * @return The result of the task execution as a map.
   */
  private Map<String, Serializable> processEvaluationTask(
      String transactionId, EvaluationTask evaluationTask, ExecutionRequest request) {
    request = request.mergeMetadata(evaluationTask.metadata());
    if (evaluationTask.preProcessor() != null) {
      var preProcessorPayload =
          executeProcessor(transactionId, evaluationTask.preProcessor(), request);
      request = request.withPayload(preProcessorPayload);
    }
    return executeTask(transactionId, evaluationTask.task(), request);
  }
}
