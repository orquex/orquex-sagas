package co.orquex.sagas.domain.event.metric;

import java.time.Instant;

/** Event indicating that a task execution has started. */
public record TaskStartedEvent(
    String flowId, String correlationId, String transactionId, String task, Instant timestamp) {}
