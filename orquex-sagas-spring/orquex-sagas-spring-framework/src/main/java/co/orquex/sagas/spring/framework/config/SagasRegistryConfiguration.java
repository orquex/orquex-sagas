package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.stage.InMemoryStageExecutorRegistry;
import co.orquex.sagas.core.task.DefaultTaskExecutor;
import co.orquex.sagas.core.task.InMemoryTaskExecutorRegistry;
import co.orquex.sagas.core.task.InMemoryTaskImplementationRegistry;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagasRegistryConfiguration {

  @Bean
  @ConditionalOnMissingBean(
      name = {"defaultTaskImplementationRegistry", "taskImplementationRegistry"})
  public Registry<TaskImplementation> defaultTaskImplementationRegistry(
      List<TaskImplementation> taskImplementations) {
    return InMemoryTaskImplementationRegistry.of(taskImplementations);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"defaultTaskExecutorRegistry", "taskExecutorRegistry"})
  public Registry<TaskExecutor> defaultTaskExecutorRegistry(
      Registry<TaskImplementation> taskImplementationRegistry) {
    final var taskExecutorList =
        List.<TaskExecutor>of(new DefaultTaskExecutor(taskImplementationRegistry));
    return InMemoryTaskExecutorRegistry.of(taskExecutorList);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"defaultStageExecutorRegistry", "stageExecutorRegistry"})
  public Registry<StageExecutor> defaultStageExecutorRegistry(List<StageExecutor> stageExecutors) {
    return InMemoryStageExecutorRegistry.of(stageExecutors);
  }
}
