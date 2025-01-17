package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.flow.AsyncWorkflowStageExecutor;
import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.spring.framework.config.event.DefaultCheckpointEventListener;
import co.orquex.sagas.spring.framework.config.event.SagasEventListenerConfiguration;
import co.orquex.sagas.spring.framework.config.event.handler.DefaultCheckpointEventListenerHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(SagasWorkflowAutoConfiguration.class)
@Import({SagasEventListenerConfiguration.class})
@ConditionalOnProperty(
    prefix = "orquex.sagas.spring.event",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SagasEventListenerAutoConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = "orquex.sagas.spring.event",
      name = "default-checkpoint-event-listener",
      havingValue = "true",
      matchIfMissing = true)
  EventListener<Checkpoint> defaultCheckpointEventListener(
      AsyncWorkflowStageExecutor workflowStageExecutor,
      CompensationExecutor compensationExecutor,
      FlowRepository flowRepository,
      TransactionRepository transactionRepository) {
    final var checkpointEventListenerHandler =
        new DefaultCheckpointEventListenerHandler(
            workflowStageExecutor, compensationExecutor, flowRepository, transactionRepository);
    return new DefaultCheckpointEventListener(checkpointEventListenerHandler);
  }
}
