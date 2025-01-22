package co.orquex.sagas.core.compensation;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the CompensationExecutor interface. It's responsible for executing
 * compensations for a given task sequentially and synchronously.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCompensationExecutor implements CompensationExecutor {

  private final Registry<TaskExecutor> taskExecutorRegistry;
  private final TaskRepository taskRepository;
  private final CompensationRepository compensationRepository;

  /**
   * Executes the compensations for the given transaction ID.
   *
   * @param transactionId the ID of the transaction for which compensations need to be executed
   * @throws WorkflowException if a task executor or task isn't found
   */
  @Override
  public void execute(String transactionId) {
    final var compensations = compensationRepository.findByTransactionId(transactionId);

    for (final var compensation : compensations) {
      final var task =
          taskRepository
              .findById(compensation.task())
              .orElseThrow(
                  () ->
                      new WorkflowException(
                          "Task '%s' not found when trying to perform compensation"
                              .formatted(compensation.task())));
      final var executorId = task.configuration().executor();
      final var taskExecutor =
          taskExecutorRegistry
              .get(executorId)
              .orElseThrow(
                  () ->
                      new WorkflowException(
                          "Task executor '%s' not found when trying to perform compensation"
                              .formatted(executorId)));
      try {
        final var executionRequest =
            new ExecutionRequest(
                compensation.flowId(),
                compensation.correlationId(),
                compensation.metadata(),
                compensation.request());
        final var compensationResponse =
            taskExecutor.execute(transactionId, task, executionRequest);

        log.debug(
            "Compensation executed for transaction '{}' and task '{}'", transactionId, task.id());
        log.trace(
            "Compensation response for transaction '{}' and task '{}': {}",
            transactionId,
            task.id(),
            compensationResponse);
      } catch (WorkflowException e) {
        log.error(
            "Compensation execution failed for transaction '{}' and task '{}'",
            transactionId,
            task.id());
      }
    }
  }
}
