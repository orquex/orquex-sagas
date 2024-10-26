package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration is used by the different stage executors to publish events either async or
 * sync.
 */
@Configuration
public class SagasWorkflowEventPublisherConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"workflowEventPublisher", "defaultWorkflowEventPublisher"})
  public WorkflowEventPublisher defaultWorkflowEventPublisher(
      EventManagerFactory defaultEventManagerFactory) {
    return new DefaultWorkflowEventPublisher(defaultEventManagerFactory);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"eventManagerFactory", "defaultEventManagerFactory"})
  public EventManagerFactory defaultEventManagerFactory() {
    return new DefaultEventManagerFactory();
  }
}
