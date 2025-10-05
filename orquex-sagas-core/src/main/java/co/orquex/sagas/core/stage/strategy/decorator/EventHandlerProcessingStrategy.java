package co.orquex.sagas.core.stage.strategy.decorator;

import co.orquex.sagas.core.event.WorkflowEventPublisher;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.StageProcessingStrategy;
import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageResponse;
import co.orquex.sagas.domain.transaction.Checkpoint;
import co.orquex.sagas.domain.transaction.Status;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventHandlerProcessingStrategy<S extends Stage> implements StageProcessingStrategy<S> {

  private final StageProcessingStrategy<S> strategy;
  private final WorkflowEventPublisher workflowEventPublisher;

  @Override
  public StageResponse process(String transactionId, S stage, ExecutionRequest request) {
    final var checkpointBuilder =
        Checkpoint.builder()
            .transactionId(transactionId)
            .flowId(request.flowId())
            .correlationId(request.correlationId())
            .stageId(stage.getId())
            .metadata(request.metadata())
            .payload(request.payload())
            .incoming(stage);
    try {
      workflowEventPublisher.publish(
          new EventMessage<>(checkpointBuilder.status(Status.IN_PROGRESS).build()));
      final var response = strategy.process(transactionId, stage, request);
      workflowEventPublisher.publish(
          new EventMessage<>(
              checkpointBuilder
                  .status(Status.COMPLETED)
                  .response(response.payload())
                  .outgoing(response.outgoing())
                  .build()));
      return response;
    } catch (WorkflowException e) {
      workflowEventPublisher.publish(
          new EventMessage<>(
              checkpointBuilder.status(Status.ERROR).build(),
              Error.builder().message(e.getMessage()).build()));
      throw e;
    }
  }
}
