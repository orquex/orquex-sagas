package co.orquex.sagas.domain.event.metric;

import java.time.Instant;

/** Event indicating that a flow execution has started. */
public record FlowStartedEvent(String flowId, String correlationId, Instant timestamp) {}
