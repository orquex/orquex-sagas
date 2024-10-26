package co.orquex.sagas.core.stage;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.stage.strategy.decorator.EventHandlerProcessingStrategy;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.stage.*;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of the {@link StageExecutor}. */
@Slf4j
public class DefaultAsyncStageExecutor extends DefaultStageExecutor {

  public DefaultAsyncStageExecutor(
      final StageProcessingStrategy<Activity> activityStrategy,
      final StageProcessingStrategy<Evaluation> evaluationStrategy,
      final WorkflowEventPublisher workflowEventPublisher) {
    super(
        decorateWithEventHandler(activityStrategy, workflowEventPublisher),
        decorateWithEventHandler(evaluationStrategy, workflowEventPublisher));
  }

  private static <T extends Stage> StageProcessingStrategy<T> decorateWithEventHandler(
      final StageProcessingStrategy<T> strategy, final WorkflowEventPublisher eventPublisher) {
    return new EventHandlerProcessingStrategy<>(strategy, eventPublisher);
  }

  @Override
  public String getKey() {
    return StageConfiguration.DEFAULT_IMPLEMENTATION;
  }
}
