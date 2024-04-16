package co.orquex.sagas.core.stage;

import co.orquex.sagas.core.stage.strategy.StageProcessingStrategy;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.StageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultStageExecutor implements ExecutableStage {

  private final StageProcessingStrategy<Activity> activityStrategy;
  private final StageProcessingStrategy<Evaluation> evaluationStrategy;

  @Override
  public void execute(StageRequest stageRequest) {
    final var request = stageRequest.executionRequest();
    final var stage = stageRequest.stage();
    try {
      switch (stage) {
        case Activity activity -> activityStrategy.process(
            stageRequest.transactionId(), activity, request);
        case Evaluation evaluation -> evaluationStrategy.process(
            stageRequest.transactionId(), evaluation, request);
        default -> throw new WorkflowException(
            "Unexpected stage: '%s' at flow '%s'".formatted(stage, request.flowId()));
      }
    } catch (WorkflowException e) {
      log.error(e.getMessage());
    }
  }
}
