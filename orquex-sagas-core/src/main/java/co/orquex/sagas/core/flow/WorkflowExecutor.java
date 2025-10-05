package co.orquex.sagas.core.flow;

import static co.orquex.sagas.domain.utils.Maps.merge;
import static java.util.Objects.nonNull;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.repository.CheckpointRepository;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.execution.ExecutionResponse;
import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageResponse;
import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Synchronous workflow executor that orchestrates saga workflows using an orchestration pattern.
 * Handles workflow execution, transaction management, and resume-from-failure capabilities through
 * checkpoint recovery. Each workflow stage is executed synchronously with configurable timeouts.
 *
 * @see AbstractWorkflowExecutor
 */
@Slf4j
public class WorkflowExecutor extends AbstractWorkflowExecutor
    implements Executable<ExecutionRequest, ExecutionResponse> {

  private final StageExecutor stageExecutor;
  private final ExecutorService executor;
  private final CompensationExecutor compensationExecutor;
  private final GlobalContext globalContext;

  /**
   * Constructs a WorkflowExecutor with the required dependencies.
   *
   * @param flowRepository the repository for flow operations
   * @param transactionRepository the repository for transaction operations
   * @param stageExecutor the executor for individual stages
   * @param compensationExecutor the executor for compensation logic
   * @param executor the thread pool executor for async operations
   * @param globalContext the global context for workflow data
   */
  public WorkflowExecutor(
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository,
      final StageExecutor stageExecutor,
      final CompensationExecutor compensationExecutor,
      final ExecutorService executor,
      final GlobalContext globalContext) {
    super(flowRepository, transactionRepository);
    this.stageExecutor = stageExecutor;
    this.executor = executor;
    this.compensationExecutor = compensationExecutor;
    this.globalContext = globalContext;
  }

  /**
   * Constructs a WorkflowExecutor with the required dependencies including checkpoint repository.
   *
   * @param flowRepository the repository for flow operations
   * @param transactionRepository the repository for transaction operations
   * @param checkpointRepository the repository for checkpoint operations, enabling resume from
   *     failure functionality
   * @param stageExecutor the executor for individual stages
   * @param compensationExecutor the executor for compensation logic
   * @param executor the thread pool executor for async operations
   * @param globalContext the global context for workflow data
   */
  public WorkflowExecutor(
      final FlowRepository flowRepository,
      final TransactionRepository transactionRepository,
      final CheckpointRepository checkpointRepository,
      final StageExecutor stageExecutor,
      final CompensationExecutor compensationExecutor,
      final ExecutorService executor,
      final GlobalContext globalContext) {
    super(flowRepository, transactionRepository, checkpointRepository);
    this.stageExecutor = stageExecutor;
    this.executor = executor;
    this.compensationExecutor = compensationExecutor;
    this.globalContext = globalContext;
  }

  /** {@inheritDoc} */
  @Override
  public ExecutionResponse execute(ExecutionRequest executionRequest) {
    if (executionRequest == null) throw new WorkflowException("Execution request required.");
    final var flow = getFlow(executionRequest.flowId());
    final var correlationId = executionRequest.correlationId();
    final var flowId = flow.id();
    log.debug("Orchestrating flow '{}' with correlation ID '{}'", flow.name(), correlationId);

    // Check transaction already exists by correlation id and is in progress status.
    final var optTransaction =
        transactionRepository.findByFlowIdAndCorrelationId(flowId, correlationId);
    if (optTransaction.isPresent()) {
      final var transaction = optTransaction.get();
      final var resumeFromFailure = flow.configuration().resumeFromFailure();
      if (resumeFromFailure && transaction.status().equals(Status.ERROR)) {
        return resumeFlow(flow, correlationId, transaction);
      } else {
        // Otherwise, throw an exception.
        throw new WorkflowException(
            "Flow '%s' with correlation ID '%s' has already been initiated."
                .formatted(flowId, correlationId));
      }
    }

    // Merge request and flow metadata.
    executionRequest = executionRequest.mergeMetadata(flow.metadata());

    // Get the initial stage from flow
    final var initialStage = getStage(flow, flow.initialStage());

    return executeFlow(flow, executionRequest, initialStage);
  }

  /**
   * Resumes workflow execution from a previous failure using checkpoint data. This method retrieves
   * the last checkpoint and continues execution from that point.
   *
   * @param flow the workflow flow definition
   * @param correlationId the correlation identifier for the transaction
   * @param transaction the existing transaction to resume
   * @return the workflow execution response
   * @throws WorkflowException if a checkpoint repository is not available or no checkpoint is found
   */
  private ExecutionResponse resumeFlow(Flow flow, String correlationId, Transaction transaction) {
    final var flowId = flow.id();
    // Checkpoint repository is required to resume from failure
    if (checkpointRepository == null) {
      throw new WorkflowException(
          "Flow '%s' with correlation id '%s' cannot be resumed from failure because checkpoint repository is not provided."
              .formatted(flowId, correlationId));
    }
    // Get the checkpoint to resume from
    final var checkpoint =
        checkpointRepository
            .findByTransactionId(transaction.transactionId())
            .orElseThrow(
                () ->
                    new WorkflowException(
                        "Flow '%s' with correlation id '%s' has no checkpoints to resume from."
                            .formatted(flowId, correlationId)));
    log.debug(
        "Resuming flow '{}' with correlation ID '{}' from checkpoint stage '{}'",
        flowId,
        correlationId,
        checkpoint.stageId());
    // Get checkpoint stage from flow's stages.
    final var resumeStage = getStage(flow, checkpoint.stageId());
    final var request =
        new ExecutionRequest(flowId, correlationId, checkpoint.metadata(), checkpoint.payload());
    // Execute the stage from the current checkpoint.
    return executeFlow(flow, request, resumeStage);
  }

  /**
   * Executes the workflow flow starting from a specified stage with comprehensive error handling.
   * This method manages the complete lifecycle of workflow execution including transaction
   * initialization, async execution submission, timeout handling, error processing, and cleanup.
   *
   * @param flow the workflow flow definition containing stages and configuration
   * @param executionRequest the execution request with metadata and payload
   * @param startingStage the stage to begin execution from (initial stage or resume stage)
   * @return the final workflow response containing output data from the last executed stage
   */
  private ExecutionResponse executeFlow(
      final Flow flow, final ExecutionRequest executionRequest, final Stage startingStage) {
    // Get the flow configuration to get the timeout duration
    final var flowConfiguration = flow.configuration();
    // Register the transaction
    var transaction = initializeTransaction(flow, executionRequest);
    // Submit the flow execution to the executor
    final Future<Map<String, Serializable>> future =
        executor.submit(
            createExecutionCallable(transaction, flow, executionRequest, startingStage));
    final var timeout = flowConfiguration.timeout();

    try {
      final var responsePayload = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      return new ExecutionResponse(transaction.transactionId(), responsePayload);
    } catch (InterruptedException e) {
      transaction = transaction.withStatus(Status.ERROR);
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new WorkflowException(
          "An error occurred while executing flow '%s'.".formatted(flow.id()));
    } catch (ExecutionException e) {
      transaction = transaction.withStatus(Status.ERROR);
      if (e.getCause() instanceof WorkflowException we) {
        throw we;
      }
      log.error(e.getMessage(), e);
      throw new WorkflowException(
          "An error occurred while executing flow '%s'.".formatted(flow.id()));
    } catch (TimeoutException e) {
      transaction = transaction.withStatus(Status.ERROR);
      throw new WorkflowException(
          "Flow '%s' timed out after %s.".formatted(flow.id(), timeout.toString()));
    } finally {
      // Execute compensation if the transaction is not completed
      if (transaction.status().equals(Status.ERROR)) {
        compensationExecutor.execute(transaction.transactionId());
      } else {
        // Clean up the context if the transaction is not in error
        globalContext.remove(transaction.transactionId());
      }
      // Update the transaction status
      transaction = transaction.withStatus(Status.COMPLETED);
      updateTransaction(transaction);
    }
  }

  /**
   * Creates a callable that executes the workflow stages sequentially starting from a specified
   * stage. This method implements the core workflow execution logic as a Callable that can be
   * submitted to an ExecutorService. It handles stage-by-stage execution, circular reference
   * detection, metadata merging, and payload transformation through the workflow chain.
   *
   * @param transaction the current transaction context for the workflow execution
   * @param flow the workflow flow definition containing all stage definitions
   * @param executionRequest the original execution request with initial metadata and payload
   * @param startingStage the stage to begin execution from (supports both new and resume scenarios)
   * @return a callable that when executed returns the final workflow payload
   */
  private Callable<Map<String, Serializable>> createExecutionCallable(
      final Transaction transaction,
      final Flow flow,
      final ExecutionRequest executionRequest,
      final Stage startingStage) {
    return () -> {
      final var callStack = new HashMap<String, String>();
      final var flowId = executionRequest.flowId();
      final var correlationId = executionRequest.correlationId();
      var metadata = executionRequest.metadata();
      var payload = executionRequest.payload();
      var nextStageId = startingStage.getId(); // Use the starting stage ID
      // Iterate over stages to execute each one.
      while (nonNull(nextStageId) && !nextStageId.isEmpty()) {
        // Check for circular execution
        if (callStack.putIfAbsent(nextStageId, nextStageId) != null) {
          throw new WorkflowException(
              "Circular execution detected in flow '%s' at stage '%s'."
                  .formatted(flowId, nextStageId));
        }
        // Get the next stage
        final var stage = getStage(flow, nextStageId);
        // Prepare the stage metadata merging the request metadata with the flow and stage metadata
        final var stageMetadata = merge(metadata, flow.metadata(), stage.getMetadata());
        final var stageExecutionRequest =
            new ExecutionRequest(flowId, correlationId, stageMetadata, payload);
        // Execute stage
        final var stageResponse = executeStage(transaction, stage, stageExecutionRequest);
        payload = stageResponse.payload();
        nextStageId = stageResponse.outgoing();
        metadata = Collections.emptyMap(); // Reset metadata to avoid leaking sensitive information
      }
      return payload;
    };
  }

  /**
   * Executes a single workflow stage and returns the stage response. This method creates a stage
   * request from the provided parameters and delegates the actual execution to the configured stage
   * executor.
   *
   * @param transaction the current transaction providing context and tracking information
   * @param stage the stage definition containing type, configuration, and execution parameters
   * @param request the execution request with current metadata and payload for this stage
   * @return the stage response containing output payload and next stage reference
   */
  private StageResponse executeStage(
      Transaction transaction, Stage stage, ExecutionRequest request) {
    final var stageRequest = getStageRequest(transaction.transactionId(), stage, request);
    return stageExecutor.execute(stageRequest);
  }
}
