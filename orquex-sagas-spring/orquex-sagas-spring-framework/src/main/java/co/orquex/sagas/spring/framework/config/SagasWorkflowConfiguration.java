package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

/** Configure the required {@link WorkflowExecutor} beans. */
@Configuration
public class SagasWorkflowConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = {"workflowExecutor"})
  public WorkflowExecutor workflowExecutor(
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository,
      @Qualifier("defaultStageExecutor") final StageExecutor defaultStageExecutor,
      final ExecutorService workflowExecutorService) {
    return new WorkflowExecutor(
        flowRepository, transactionRepository, defaultStageExecutor, workflowExecutorService);
  }

  @Bean
  @ConditionalOnMissingBean(name = {"workflowExecutorService"})
  public ExecutorService workflowExecutorService() {
    return Executors.newFixedThreadPool(10, r -> new Thread(r, "workflow-executor-"));
  }
}
