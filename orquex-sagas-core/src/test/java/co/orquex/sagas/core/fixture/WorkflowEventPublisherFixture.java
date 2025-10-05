package co.orquex.sagas.core.fixture;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkflowEventPublisherFixture {

  public static WorkflowEventPublisher getWorkflowEventPublisher(
      EventManagerFactory eventManagerFactory) {
    return new DefaultWorkflowEventPublisher(eventManagerFactory);
  }
}
