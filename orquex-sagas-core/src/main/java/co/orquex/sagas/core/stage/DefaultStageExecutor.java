package co.orquex.sagas.core.stage;

import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.stage.*;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of the {@link StageExecutor}. */
@Slf4j
public class DefaultStageExecutor implements StageExecutor {

  public static final String DEFAULT_SYNC_STAGE_EXECUTOR_KEY = "default-sync";
  private final StageProcessingStrategy<Activity> activityStrategy;
  private final StageProcessingStrategy<Evaluation> evaluationStrategy;

  public DefaultStageExecutor(
      final StageProcessingStrategy<Activity> activityStrategy,
      final StageProcessingStrategy<Evaluation> evaluationStrategy) {
    this.activityStrategy = activityStrategy;
    this.evaluationStrategy = evaluationStrategy;
  }

  @Override
  public StageResponse execute(StageRequest stageRequest) {
    final var request = stageRequest.executionRequest();
    final var stage = stageRequest.stage();
    log.trace(
        "Executing default stage '{}' for flow '{}' and correlation ID '{}'",
        stage.getId(),
        request.flowId(),
        request.correlationId());
    return switch (stage) {
      case Activity activity ->
          activityStrategy.process(stageRequest.transactionId(), activity, request);
      case Evaluation evaluation ->
          evaluationStrategy.process(stageRequest.transactionId(), evaluation, request);
      default ->
          throw new WorkflowException(
              "Unexpected stage '%s' at flow '%s'".formatted(stage, request.flowId()));
    };
  }

  @Override
  public String getKey() {
    return DEFAULT_SYNC_STAGE_EXECUTOR_KEY;
  }
}
