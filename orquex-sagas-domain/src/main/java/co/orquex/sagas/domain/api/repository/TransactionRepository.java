package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.transaction.Transaction;

public interface TransactionRepository {

  boolean existsByFlowIdAndCorrelationId(String flowId, String correlationId);
  Transaction save(Transaction transaction);
}
