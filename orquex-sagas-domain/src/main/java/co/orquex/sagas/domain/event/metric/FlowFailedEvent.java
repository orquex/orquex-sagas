package co.orquex.sagas.domain.event.metric;

import java.time.Duration;
import java.time.Instant;

/** Event indicating that a flow execution has failed. */
public record FlowFailedEvent(
    String flowId, String correlationId, Instant timestamp, Duration duration) {}
