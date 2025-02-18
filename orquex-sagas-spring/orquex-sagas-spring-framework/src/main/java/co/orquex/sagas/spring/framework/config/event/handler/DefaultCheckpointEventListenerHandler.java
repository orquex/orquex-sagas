package co.orquex.sagas.spring.framework.config.event.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import co.orquex.sagas.core.flow.AsyncWorkflowStageExecutor;
import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link CheckpointEventListenerHandler} that handles the different
 * status of a {@link Checkpoint} and executes the next stage when there's an outgoing and the
 * previous one was completed.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCheckpointEventListenerHandler implements CheckpointEventListenerHandler {

  private final AsyncWorkflowStageExecutor workflowStageExecutor;
  private final CompensationExecutor compensationExecutor;
  private final FlowRepository flowRepository;
  private final TransactionRepository transactionRepository;

  public void handle(Checkpoint checkpoint) {
    log.trace(
        "Listener Handler received checkpoint for '{}' and correlation ID '{}' at stage '{}' with status '{}'",
        checkpoint.flowId(),
        checkpoint.correlationId(),
        checkpoint.incoming().getName(),
        checkpoint.status());
    switch (checkpoint.status()) {
      case COMPLETED -> handleCheckpointCompleted(checkpoint);
      case ERROR -> handleCheckpointError(checkpoint);
      case CANCELED -> handleCheckpointCanceled(checkpoint);
      case IN_PROGRESS -> handleCheckpointInProgress(checkpoint);
      default ->
          throw new WorkflowException("Unexpected checkpoint status: " + checkpoint.status());
    }
  }

  private void handleCheckpointCompleted(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
    if (nonNull(checkpoint.outgoing())) {
      workflowStageExecutor.execute(checkpoint);
    } else {
      final var transaction = getTransaction(checkpoint.transactionId());
      transaction.setStatus(Status.COMPLETED);
      transactionRepository.save(transaction);
      log.info(
          "Flow '{}' with correlation id '{}' has been completed",
          checkpoint.flowId(),
          checkpoint.correlationId());
    }
  }

  private void handleCheckpointError(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
    final var flow = getFlow(checkpoint.flowId());
    final var allOrNothing = flow.configuration().allOrNothing();
    if (allOrNothing || isNull(checkpoint.outgoing())) {
      // If it is all or nothing or there is not outgoing then executes the compensation
      // Also updates the transaction status
      final var transaction = getTransaction(checkpoint.transactionId());
      transaction.setStatus(Status.ERROR);
      transactionRepository.save(transaction);
      log.info(
          "Flow '{}' with correlation ID '{}' has been completed with error at stage '{}'",
          checkpoint.flowId(),
          checkpoint.correlationId(),
          checkpoint.incoming().getName());
      compensationExecutor.execute(checkpoint.transactionId());
    } else {
      // If it is not all or nothing and there is outgoing, then it will be executed by the next stage
      workflowStageExecutor.execute(checkpoint);
    }
  }

  private void handleCheckpointCanceled(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
    final var transaction = getTransaction(checkpoint.transactionId());
    transaction.setStatus(Status.CANCELED);
    transactionRepository.save(transaction);
    log.info(
            "Flow '{}' with correlation ID '{}' has been cancelled by stage '{}'",
            checkpoint.flowId(),
            checkpoint.correlationId(),
            checkpoint.incoming().getName());
    compensationExecutor.execute(checkpoint.transactionId());
  }

  private void handleCheckpointInProgress(Checkpoint checkpoint) {
    log.trace(getCheckpointStatus(checkpoint));
  }

  private String getCheckpointStatus(Checkpoint checkpoint) {
    return "Handling checkpoint '%s' for '%s' in '%s' with correlation ID '%s'"
        .formatted(
            checkpoint.status().name(),
            checkpoint.incoming().getName(),
            checkpoint.flowId(),
            checkpoint.correlationId());
  }

  protected Flow getFlow(String flowId) {
    return flowRepository
        .findById(flowId)
        .orElseThrow(() -> new WorkflowException(String.format("Flow '%s' not found.", flowId)));
  }

  private Transaction getTransaction(String transactionId) {
    return transactionRepository
        .findById(transactionId)
        .orElseThrow(
            () -> new WorkflowException("Transaction '%s' not found.".formatted(transactionId)));
  }
}
