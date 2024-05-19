package co.orquex.sagas.core.stage.strategy.impl;

import static co.orquex.sagas.domain.stage.Evaluation.EXPRESSION;
import static co.orquex.sagas.domain.stage.Evaluation.RESULT;

import co.orquex.sagas.core.stage.strategy.StrategyResponse;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.stage.Condition;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.EvaluationTask;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;

@Slf4j
public class EvaluationProcessingStrategy extends AbstractStageProcessingStrategy<Evaluation> {

  public EvaluationProcessingStrategy(
      Registry<TaskExecutor> taskExecutorRegistry, TaskRepository taskRepository) {
    super(taskExecutorRegistry, taskRepository);
  }

  @Override
  public StrategyResponse process(
      String transactionId, Evaluation evaluation, ExecutionRequest request) {
    log.debug("Executing evaluation stage '{}'", evaluation.getName());
    var outgoing = evaluation.getDefaultOutgoing();

    for (Condition condition : evaluation.getConditions()) {
      // add the condition expression to the metadata
      request = request.mergeMetadata(evaluation.getMetadata());
      request.metadata().put(EXPRESSION, condition.expression());
      // Call task
      var response = processEvaluationTask(transactionId, evaluation.getEvaluationTask(), request);
      // get the evaluation result
      if (response.containsKey(RESULT)
          && response.get(RESULT) instanceof Boolean result
          && result) {
        outgoing = condition.outgoing();
        break;
      }
    }

    // return the default outgoing.
    return StrategyResponse.builder().outgoing(outgoing).payload(request.payload()).build();
  }

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
