package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.DefaultWorkflowEventPublisher;
import co.orquex.sagas.core.event.manager.EventManagerFactory;
import co.orquex.sagas.core.event.manager.impl.DefaultEventManagerFactory;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.DefaultStageExecutor;
import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.core.task.DefaultTaskExecutor;
import co.orquex.sagas.core.task.InMemoryTaskExecutorRegistry;
import co.orquex.sagas.core.task.InMemoryTaskImplementationRegistry;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the required Stage beans. */
@Configuration
public class SagasStageConfiguration {

  @Bean
  public Registry<TaskImplementation> defaultTaskImplementationRegistry(
      List<TaskImplementation> taskImplementations) {
    return InMemoryTaskImplementationRegistry.of(taskImplementations);
  }

  @Bean
  public Registry<TaskExecutor> defaultTaskExecutorRegistry(
      Registry<TaskImplementation> taskImplementationRegistry) {
    final var taskExecutorList =
        List.<TaskExecutor>of(new DefaultTaskExecutor(taskImplementationRegistry));
    return InMemoryTaskExecutorRegistry.of(taskExecutorList);
  }

  @Bean
  public Registry<StageExecutor> defaultStageExecutorRegistry(List<StageExecutor> stageExecutors) {
    return InMemoryStageExecutorRegistry.of(stageExecutors);
  }

  @Bean
  public EventListener<StageRequest> defaultStageEventListener(
      Registry<StageExecutor> stageExecutorRegistry) {
    return new DefaultStageEventListener(stageExecutorRegistry);
  }

  @Bean
  public StageExecutor defaultStageExecutor(
      Registry<TaskExecutor> defaultTaskExecutorRegistry,
      TaskRepository taskRepository,
      WorkflowEventPublisher defaultWorkflowEventPublisher) {
    // Decorate the strategies' implementations with an event handler
    final var activityStrategy =
        new ActivityProcessingStrategy(
            defaultTaskExecutorRegistry, taskRepository, defaultWorkflowEventPublisher);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(
            defaultTaskExecutorRegistry, taskRepository, defaultWorkflowEventPublisher);
    return new DefaultStageExecutor(
        activityStrategy, evaluationStrategy, defaultWorkflowEventPublisher);
  }

  @Bean
  public WorkflowEventPublisher defaultWorkflowEventPublisher(
      EventManagerFactory defaultEventManagerFactory) {
    return new DefaultWorkflowEventPublisher(defaultEventManagerFactory);
  }

  @Bean
  public EventManagerFactory defaultEventManagerFactory() {
    return new DefaultEventManagerFactory();
  }
}
