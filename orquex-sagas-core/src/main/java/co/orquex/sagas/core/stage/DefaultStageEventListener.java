package co.orquex.sagas.core.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;
import static java.util.Objects.isNull;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.stage.StageRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link EventListener} interface for handling stage requests and
 * executing with the appropriate stage executor.
 */
@Slf4j
public class DefaultStageEventListener implements EventListener<StageRequest> {

  private final Registry<StageExecutor> stageRegistry;

  public DefaultStageEventListener(Registry<StageExecutor> stageExecutorRegistry) {
    this.stageRegistry =
        checkArgumentNotNull(stageExecutorRegistry, "Stage executor registry cannot be null");
  }

  @Override
  public void onMessage(EventMessage<StageRequest> message) {
    final var request = message.message();
    if (isNull(request)) {
      log.warn("Ignored a null stage request received");
      return;
    }
    final var stage = request.stage();
    if (isNull(stage)) {
      log.warn("Ignored a null stage received");
      return;
    }
    final var impl = stage.getConfiguration().implementation();
    stageRegistry
        .get(impl)
        .ifPresentOrElse(
            stageExecutor -> stageExecutor.execute(request),
            () -> log.error("Stage executor '{}' not found", impl));
  }

  @Override
  public void onError(EventMessage<StageRequest> message) {
    log.error("Receiving stage request error: {}", message.error());
  }
}
