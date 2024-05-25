package co.orquex.sagas.sample.config;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.core.flow.WorkflowStageExecutor;
import co.orquex.sagas.core.stage.DefaultStageEventListener;
import co.orquex.sagas.core.stage.DefaultStageExecutor;
import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.core.task.DefaultTaskExecutor;
import co.orquex.sagas.core.task.InMemoryTaskExecutorRegistry;
import co.orquex.sagas.core.task.InMemoryTaskImplementationRegistry;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import co.orquex.sagas.domain.registry.Registry;
import co.orquex.sagas.domain.repository.FlowRepository;
import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.sample.event.CheckpointListener;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // For managing async event handling
public class SagasConfig {

  @Bean
  public WorkflowExecutor workflowExecutor(
      final EventManager<StageRequest> stageRequestEventManager,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new WorkflowExecutor(stageRequestEventManager, flowRepository, transactionRepository);
  }

  @Bean
  public WorkflowStageExecutor workflowHandler(
      final EventManager<StageRequest> stageRequestEventManager,
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository) {
    return new WorkflowStageExecutor(
        stageRequestEventManager, flowRepository, transactionRepository);
  }

  @Bean
  public DefaultStageExecutor defaultStageExecutor(
      final Registry<TaskExecutor> taskExecutorRegistry,
      final TaskRepository taskRepository,
      final EventManager<Checkpoint> eventManager) {
    // Decorate the strategy implementation with an event handler
    final var activityStrategy =
        new ActivityProcessingStrategy(taskExecutorRegistry, taskRepository);
    final var evaluationStrategy =
        new EvaluationProcessingStrategy(taskExecutorRegistry, taskRepository);
    return new DefaultStageExecutor(activityStrategy, evaluationStrategy, eventManager);
  }

  @Bean
  public EventManager<Checkpoint> checkpointEventManager(
      ApplicationEventPublisher applicationEventPublisher) {
    final var eventManager = new EventManager<Checkpoint>();
    eventManager.addListener(new CheckpointListener(applicationEventPublisher));
    return eventManager;
  }

  @Bean
  public EventManager<StageRequest> stageRequestEventManager(
      DefaultStageExecutor defaultStageExecutor) {
    final var eventManager = new EventManager<StageRequest>();
    final var stageExecutorRegistry =
        InMemoryStageExecutorRegistry.of(List.of(defaultStageExecutor));
    eventManager.addListener(new DefaultStageEventListener(stageExecutorRegistry));
    return eventManager;
  }

  @Bean
  public Registry<TaskExecutor> taskExecutorRegistry(
      Registry<TaskImplementation> taskImplementationRegistry) {
    return InMemoryTaskExecutorRegistry.of(
        List.of(new DefaultTaskExecutor(taskImplementationRegistry)));
  }

  @Bean
  public Registry<TaskImplementation> taskImplementationRegistry(
      List<TaskImplementation> taskImplementations) {
    final var tasks = new ArrayList<>(taskImplementations);
    tasks.add(new GroovyActivity()); // Built in tasks implementations
    tasks.add(new GroovyEvaluation()); // Built in tasks implementations
    return InMemoryTaskImplementationRegistry.of(tasks);
  }

  /** Add jackson support for the sagas to the current object mapper */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.modulesToInstall(new OrquexJacksonModule());
  }
}
