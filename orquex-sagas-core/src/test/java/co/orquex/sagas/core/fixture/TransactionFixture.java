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
    return new Transaction(
        UUID.randomUUID().toString(),
        flowId,
        CORRELATION_ID,
        null,
        status,
        Instant.now(),
        Instant.now(),
        Instant.now().plusSeconds(30));
  }
}
