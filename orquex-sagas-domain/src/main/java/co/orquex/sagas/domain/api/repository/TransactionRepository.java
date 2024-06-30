package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.transaction.Transaction;

public interface TransactionRepository {

  boolean existByFlowIdAndCorrelationId(String flowId, String correlationId);
  Transaction save(Transaction transaction);
}
