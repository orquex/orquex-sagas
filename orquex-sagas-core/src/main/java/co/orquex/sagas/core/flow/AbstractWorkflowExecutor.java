package co.orquex.sagas.core.flow;

import co.orquex.sagas.domain.api.repository.CheckpointRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageRequest;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for workflow executors that provides a common capability or feature for
 * executing workflows.
 */
abstract class AbstractWorkflowExecutor {

  protected final FlowRepository flowRepository;
  protected final TransactionRepository transactionRepository;
  protected final CheckpointRepository checkpointRepository;

  protected AbstractWorkflowExecutor(
      FlowRepository flowRepository,
      TransactionRepository transactionRepository,
      CheckpointRepository checkpointRepository) {
    this.flowRepository = flowRepository;
    this.transactionRepository = transactionRepository;
    this.checkpointRepository = checkpointRepository;
  }

  protected AbstractWorkflowExecutor(
      FlowRepository flowRepository, TransactionRepository transactionRepository) {
    this(flowRepository, transactionRepository, null);
  }

  /**
   * Get the flow by its ID from the repository.
   *
   * @param flowId the flow ID.
   * @return the flow store in the repository.
   */
  protected Flow getFlow(String flowId) {
    return flowRepository
        .findById(flowId)
        .orElseThrow(() -> new WorkflowException(String.format("Flow '%s' not found.", flowId)));
  }

  /**
   * Get the stage by its ID from the flow's stage map.
   *
   * @param flow the flow that contains the stage.
   * @param stageId the stage ID.
   * @return the stage from the flow.
   */
  protected Stage getStage(Flow flow, String stageId) {
    final var stages = flow.stages();
    if (stages.containsKey(stageId)) {
      return stages.get(stageId);
    }
    throw new WorkflowException(
        String.format("Stage '%s' not found in flow '%s'.", stageId, flow.id()));
  }

  /**
   * Build the stage request to be executed.
   *
   * @param transactionId the transaction ID.
   * @param stage the stage to be executed.
   * @param request the execution request.
   * @return the stage request.
   */
  protected StageRequest getStageRequest(
      String transactionId, Stage stage, ExecutionRequest request) {
    return StageRequest.builder()
        .transactionId(transactionId)
        .stage(stage)
        .executionRequest(request)
        .build();
  }

  /**
   * Initialize a new transaction for the flow and store the transaction in its repository.
   *
   * @param flow the flow to be executed.
   * @param request the execution request.
   * @return the initialized transaction.
   */
  protected Transaction initializeTransaction(final Flow flow, final ExecutionRequest request) {
    final var transaction =
        new Transaction(
            UUID.randomUUID().toString(),
            flow.id(),
            request.correlationId(),
            request,
            Status.IN_PROGRESS,
            Instant.now(),
            Instant.now(),
            Instant.now().plus(flow.configuration().timeout()));
    return transactionRepository.save(transaction);
  }

  /**
   * Update the transaction in the repository.
   *
   * @param transaction the transaction to be updated.
   */
  protected void updateTransaction(Transaction transaction) {
    transactionRepository.save(transaction);
  }
}
