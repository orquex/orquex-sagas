package co.orquex.sagas.domain.task;

import java.io.Serializable;
import java.util.Map;

public record TaskRequest(
    String transactionId,
    Map<String, ? extends Serializable> metadata,
    Map<String, ? extends Serializable> payload) {}
