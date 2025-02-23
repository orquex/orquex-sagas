package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Compensation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;

/** Loads the listeners after the context has been loaded. */
@RequiredArgsConstructor
public class SagasContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

  private final EventManagerFactory eventManagerFactory;
  private final List<EventListener<Checkpoint>> checkpointEventListeners;
  private final List<EventListener<StageRequest>> stageRequestEventListeners;
  private final List<EventListener<Compensation>> compensationEventListeners;

  @Override
  public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
    // Adding EventListener<Checkpoint> from context
    checkpointEventListeners.forEach(
        checkpointEventListener ->
            eventManagerFactory
                .getEventManager(Checkpoint.class)
                .addListener(checkpointEventListener));

    // Adding EventListener<StageRequest> from context
    stageRequestEventListeners.forEach(
        stageRequestEventListener ->
            eventManagerFactory
                .getEventManager(StageRequest.class)
                .addListener(stageRequestEventListener));

    // Adding EventListener<Compensation> from context
    compensationEventListeners.forEach(
        compensationEventListener ->
            eventManagerFactory
                .getEventManager(Compensation.class)
                .addListener(compensationEventListener));
  }
}
