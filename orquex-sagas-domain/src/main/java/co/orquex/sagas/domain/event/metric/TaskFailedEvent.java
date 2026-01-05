package co.orquex.sagas.domain.event.metric;

import java.time.Duration;
import java.time.Instant;

/** Event indicating that a task execution has failed. */
public record TaskFailedEvent(
    String flowId,
    String correlationId,
    String transactionId,
    String task,
    Instant timestamp,
    Duration duration) {}
