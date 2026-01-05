package co.orquex.sagas.domain.event.metric;

import java.time.Instant;

/** Event indicating that a stage execution has started. */
public record StageStartedEvent(
    String flowId, String correlationId, String transactionId, String stage, Instant timestamp) {}
