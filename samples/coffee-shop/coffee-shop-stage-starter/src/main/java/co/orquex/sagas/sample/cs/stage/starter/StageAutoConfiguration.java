package co.orquex.sagas.sample.cs.stage.starter;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.EventManager;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManager;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.stage.DefaultStageExecutor;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.core.task.DefaultTaskExecutor;
import co.orquex.sagas.core.task.InMemoryTaskExecutorRegistry;
import co.orquex.sagas.core.task.InMemoryTaskImplementationRegistry;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.transaction.Checkpoint;
import java.util.List;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StageAutoConfiguration {

  @Bean
  public Registry<TaskImplementation> taskImplementationRegistry(
      List<TaskImplementation> taskImplementations) {
    return InMemoryTaskImplementationRegistry.of(taskImplementations);
  }

  @Bean
  public Registry<TaskExecutor> taskExecutorRegistry(
      Registry<TaskImplementation> taskImplementationRegistry) {
    final var taskExecutorList =
        List.<TaskExecutor>of(new DefaultTaskExecutor(taskImplementationRegistry));
    return InMemoryTaskExecutorRegistry.of(taskExecutorList);
  }

  @Bean
  public EventManager<Checkpoint> checkpointEventManager(
      EventListener<Checkpoint> checkpointEventListener) {
    final var eventManager = new DefaultEventManager<Checkpoint>();
    eventManager.addListener(checkpointEventListener);
    return eventManager;
  }

  @Bean
  public DefaultStageExecutor defaultStageExecutor(
      final Registry<TaskExecutor> taskExecutorRegistry,
      final TaskRepository taskRepository,
      final WorkflowEventPublisher workflowEventPublisher) {
    // Decorate the strategy implementation with an event handler
    final var activityStrategy =
        new ActivityProcessingStrategy(
            taskExecutorRegistry, taskRepository, workflowEventPublisher);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(
            taskExecutorRegistry, taskRepository, workflowEventPublisher);
    return new DefaultStageExecutor(activityStrategy, evaluationStrategy, workflowEventPublisher);
  }

  @Bean
  public WorkflowEventPublisher workflowEventPublisher(EventManagerFactory eventManagerFactory) {
    return new DefaultWorkflowEventPublisher(eventManagerFactory);
  }

  @Bean
  public EventManagerFactory eventManagerFactory() {
    return new DefaultEventManagerFactory();
  }

  /** Add jackson support for the sagas to the current object mapper */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.modulesToInstall(new OrquexJacksonModule());
  }
}
