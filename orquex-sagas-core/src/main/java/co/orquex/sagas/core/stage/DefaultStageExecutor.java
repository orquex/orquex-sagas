package co.orquex.sagas.core.stage;

import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.stage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of the {@link StageExecutor}. */
@Slf4j
@RequiredArgsConstructor
public class DefaultStageExecutor implements StageExecutor {

  public static final String DEFAULT_SYNC_STAGE_EXECUTOR_KEY = "default-sync";
  private final StageProcessingStrategy<Activity> activityStrategy;
  private final StageProcessingStrategy<Evaluation> evaluationStrategy;

  @Override
  public StageResponse execute(StageRequest stageRequest) {
    final var request = stageRequest.executionRequest();
    final var stage = stageRequest.stage();
    log.trace(
        "Processing stage '{}' for flow '{}' and correlation ID '{}'",
        stage.getName(),
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
