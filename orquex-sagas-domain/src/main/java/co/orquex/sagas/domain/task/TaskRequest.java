package co.orquex.sagas.domain.task;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a request for a task execution within a saga.
 *
 * @param transactionId Unique identifier for the transaction associated with the task.
 * @param metadata Additional metadata related to the task, as key-value pairs.
 * @param payload The main data payload for the task, as key-value pairs.
 */
public record TaskRequest(
    String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {}
