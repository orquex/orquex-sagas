package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.transaction.Status;
import co.orquex.sagas.domain.transaction.Transaction;
import java.time.Instant;
import java.util.UUID;

public final class TransactionFixture {

  private TransactionFixture() {}

  public static Transaction getTransaction() {
    final var transaction = new Transaction();
    transaction.setTransactionId(UUID.randomUUID().toString());
    transaction.setFlowId("flow-id");
    transaction.setCorrelationId("correlation-id");
    transaction.setStatus(Status.IN_PROGRESS);
    transaction.setExpiresAt(Instant.now().plusSeconds(30));
    transaction.setStartedAt(Instant.now());
    return transaction;
  }
}
