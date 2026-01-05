package co.orquex.sagas.domain.event.metric;

import java.time.Duration;
import java.time.Instant;

/** Event indicating that a stage execution has completed. */
public record StageCompletedEvent(
    String flowId,
    String correlationId,
    String transactionId,
    String stage,
    Instant timestamp,
    Duration duration) {}
