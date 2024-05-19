package co.orquex.sagas.core.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;
import static java.util.Objects.isNull;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.stage.StageRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultStageEventListener implements EventListener<StageRequest> {

  private final ExecutableStage executableStage;

  public DefaultStageEventListener(ExecutableStage executableStage) {
    this.executableStage =
        checkArgumentNotNull(executableStage, "ExecutableStage instance cannot be null");
  }

  @Override
  public void onMessage(EventMessage<StageRequest> message) {
    final var request = message.getMessage();
    if (isNull(request)) {
      log.warn("Ignored a null stage request received");
      return;
    }
    executableStage.execute(message.getMessage());
  }

  @Override
  public void onError(EventMessage<StageRequest> message) {
    log.error("Receiving stage request error: {}", message.getError());
  }
}
