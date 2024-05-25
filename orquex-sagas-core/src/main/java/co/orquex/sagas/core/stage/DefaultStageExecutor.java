package co.orquex.sagas.core.stage;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.core.stage.strategy.StageProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.decorator.EventHandlerProcessingStrategy;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.StageConfiguration;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultStageExecutor implements StageExecutor {

  private final StageProcessingStrategy<Activity> activityStrategy;
  private final StageProcessingStrategy<Evaluation> evaluationStrategy;

  public DefaultStageExecutor(
      final StageProcessingStrategy<Activity> activityStrategy,
      final StageProcessingStrategy<Evaluation> evaluationStrategy) {
    this.activityStrategy = activityStrategy;
    this.evaluationStrategy = evaluationStrategy;
  }

  public DefaultStageExecutor(
      final StageProcessingStrategy<Activity> activityStrategy,
      final StageProcessingStrategy<Evaluation> evaluationStrategy,
      final EventManager<Checkpoint> eventManager) {
    this.activityStrategy = new EventHandlerProcessingStrategy<>(activityStrategy, eventManager);
    this.evaluationStrategy =
        new EventHandlerProcessingStrategy<>(evaluationStrategy, eventManager);
  }

  @Override
  public void execute(StageRequest stageRequest) {
    final var request = stageRequest.executionRequest();
    final var stage = stageRequest.stage();
    try {
      switch (stage) {
        case Activity activity ->
            activityStrategy.process(stageRequest.transactionId(), activity, request);
        case Evaluation evaluation ->
            evaluationStrategy.process(stageRequest.transactionId(), evaluation, request);
        default ->
            throw new WorkflowException(
                "Unexpected stage: '%s' at flow '%s'".formatted(stage, request.flowId()));
      }
    } catch (WorkflowException e) {
      log.error(e.getMessage());
    }
  }

  @Override
  public String getId() {
    return StageConfiguration.DEFAULT_IMPLEMENTATION;
  }
}
