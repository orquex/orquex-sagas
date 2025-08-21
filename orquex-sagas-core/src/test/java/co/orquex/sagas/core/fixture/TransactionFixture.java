package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;

public final class TransactionFixture {

  public static final String FLOW_ID = "flow-id";
  public static final String CORRELATION_ID = "correlation-id";

  private TransactionFixture() {}

  public static Transaction getTransaction() {
    return getTransaction(FLOW_ID, Status.IN_PROGRESS);
  }

  public static Transaction getTransaction(String flowId, Status status) {
    final var transaction = new Transaction();
    transaction.setTransactionId(UUID.randomUUID().toString());
    transaction.setFlowId(flowId);
    transaction.setCorrelationId(CORRELATION_ID);
    transaction.setStatus(status);
    transaction.setExpiresAt(Instant.now().plusSeconds(30));
    transaction.setStartedAt(Instant.now());
    return transaction;
  }
}
