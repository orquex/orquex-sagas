package co.orquex.sagas.core.flow;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.stage.StageResponse;

public class WorkflowListener implements EventListener<StageResponse> {

    @Override
    public void onMessage(EventMessage<StageResponse> message) {
        // Check if transaction is expired
        // Call the new stage
    }

    @Override
    public void onError(EventMessage<StageResponse> message) {
        // Check with circuit breaker
        // Check retry
        // Check timeout
    }
}
