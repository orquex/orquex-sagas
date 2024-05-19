package co.orquex.sagas.core.stage.strategy.impl.decorator;

import co.orquex.sagas.core.event.EventManager;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.stage.strategy.StageProcessingStrategy;
import co.orquex.sagas.core.stage.strategy.StrategyResponse;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventHandlerProcessingStrategy<S extends Stage> implements StageProcessingStrategy<S> {

  private final StageProcessingStrategy<S> strategy;
  private final EventManager<Checkpoint> eventManager;

  @Override
  public StrategyResponse process(String transactionId, S stage, ExecutionRequest request) {
    final var checkpointBuilder =
        Checkpoint.builder()
            .transactionId(transactionId)
            .flowId(request.flowId())
            .correlationId(request.correlationId())
            .metadata(request.metadata())
            .request(request.payload())
            .incoming(stage);
    try {
      eventManager.send(checkpointBuilder.status(Status.IN_PROGRESS).build());
      final var response = strategy.process(transactionId, stage, request);
      eventManager.send(
          checkpointBuilder
              .status(Status.COMPLETED)
              .response(response.payload())
              .outgoing(response.outgoing())
              .build());
      return response;
    } catch (WorkflowException e) {
      eventManager.send(
          EventMessage.<Checkpoint>builder()
              .message(checkpointBuilder.status(Status.ERROR).build())
              .error(Error.builder().message(e.getMessage()).build())
              .build());
      throw e;
    }
  }
}
