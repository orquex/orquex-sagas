package co.orquex.sagas.domain.task;

import java.io.Serializable;
import java.util.Map;

public record TaskRequest(
    String transactionId,
    Map<String, Serializable> metadata,
    Map<String, Serializable> payload) {}
