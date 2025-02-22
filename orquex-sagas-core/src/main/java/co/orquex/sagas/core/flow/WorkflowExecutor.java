package co.orquex.sagas.core.flow;

import static co.orquex.sagas.domain.utils.Maps.merge;
import static java.util.Objects.nonNull;

import co.orquex.sagas.domain.api.CompensationExecutor;
import co.orquex.sagas.domain.api.Executable;
import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
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

/** Workflow synchronous executor that orchestrates the execution of its stages. */
@Slf4j
public class WorkflowExecutor extends AbstractWorkflowExecutor
    implements Executable<ExecutionRequest, Map<String, Serializable>> {

  private final StageExecutor stageExecutor;
  private final ExecutorService executor;
  private final CompensationExecutor compensationExecutor;
  private final GlobalContext globalContext;

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
   * Execute the orchestrator flow with the provided execution request.
   *
   * @param executionRequest the execution request.
   * @return the flow response.
   */
  @Override
  public Map<String, Serializable> execute(ExecutionRequest executionRequest) {
    if (executionRequest == null) throw new WorkflowException("Execution request required.");
    final var flow = getFlow(executionRequest.flowId());
    log.debug(
        "Orchestrating flow '{}' with correlation ID '{}'",
        flow.id(),
        executionRequest.correlationId());
    if (transactionRepository.existsByFlowIdAndCorrelationId(
        flow.id(), executionRequest.correlationId())) {
      throw new WorkflowException(
          "Flow '%s' with correlation ID '%s' has already been initiated."
              .formatted(executionRequest.flowId(), executionRequest.correlationId()));
    }

    return executeFlow(flow, executionRequest);
  }

  /**
   * Execute the given flow with the provided request.
   *
   * @param flow the flow to execute.
   * @param executionRequest the execution request.
   * @return the flow response.
   */
  private Map<String, Serializable> executeFlow(
      final Flow flow, final ExecutionRequest executionRequest) {
    // Get the flow configuration to get the timeout duration
    final var flowConfiguration = flow.configuration();
    // Register the transaction
    final var transaction = initializeTransaction(flow, executionRequest);
    // Submit the flow execution to the executor
    final Future<Map<String, Serializable>> future =
        executor.submit(createExecutionCallable(transaction, flow, executionRequest));
    final var timeout = flowConfiguration.timeout();

    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      transaction.setStatus(Status.ERROR);
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new WorkflowException(
          "An error occurred while executing flow '%s'.".formatted(flow.id()));
    } catch (ExecutionException e) {
      transaction.setStatus(Status.ERROR);
      if (e.getCause() instanceof WorkflowException we) {
        throw we;
      }
      log.error(e.getMessage(), e);
      throw new WorkflowException(
          "An error occurred while executing flow '%s'.".formatted(flow.id()));
    } catch (TimeoutException e) {
      transaction.setStatus(Status.ERROR);
      throw new WorkflowException(
          "Flow '%s' timed out after %s.".formatted(flow.id(), timeout.toString()));
    } finally {
      // Execute compensation if the transaction is not completed
      if (transaction.getStatus().equals(Status.ERROR)) {
        compensationExecutor.execute(transaction.getTransactionId());
      } else {
        // Clean up the context if the transaction is not in error
        globalContext.remove(transaction.getTransactionId());
      }
      // Update the transaction status
      updateTransaction(transaction);
    }
  }

  /**
   * Create a callable to execute the flow in the given executor.
   *
   * @param flow the flow to execute.
   * @param executionRequest the execution request.
   * @return the callable to execute the flow.
   */
  private Callable<Map<String, Serializable>> createExecutionCallable(
      final Transaction transaction, final Flow flow, final ExecutionRequest executionRequest) {
    return () -> {
      final var callStack = new HashMap<String, String>();
      final var flowId = executionRequest.flowId();
      final var correlationId = executionRequest.correlationId();
      var metadata = executionRequest.metadata();
      var payload = executionRequest.payload();
      var nextStageId = flow.initialStage();
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
   * Execute the given stage with the provided request.
   *
   * @param transaction the current transaction.
   * @param stage the stage to execute.
   * @param request the execution request.
   * @return the stage response.
   */
  private StageResponse executeStage(
      Transaction transaction, Stage stage, ExecutionRequest request) {
    final var stageRequest = getStageRequest(transaction.getTransactionId(), stage, request);
    return stageExecutor.execute(stageRequest);
  }
}
