package co.orquex.sagas.spring.framework.config.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.flow.WorkflowStageExecutor;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.spring.framework.config.event.handler.CheckpointEventListenerHandler;
import co.orquex.sagas.spring.framework.config.event.handler.DefaultCheckpointEventListenerHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the required Event Listener beans. */
@Configuration
public class SagasEventListenerConfiguration {

  @Bean
  EventListener<Checkpoint> defaultCheckpointEventListener(
      CheckpointEventListenerHandler checkpointEventListenerHandler) {
    return new DefaultCheckpointEventListener(checkpointEventListenerHandler);
  }

  @Bean
  CheckpointEventListenerHandler defaultCheckpointEventListenerHandler(
      WorkflowStageExecutor workflowStageExecutor) {
    return new DefaultCheckpointEventListenerHandler(workflowStageExecutor);
  }
}
