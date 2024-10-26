package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.transaction.Transaction;

/** Repository for managing transactions. */
public interface TransactionRepository {

  /**
   * Check if a transaction exists by flow ID and correlation ID.
   *
   * @param flowId the flow ID.
   * @param correlationId the correlation ID.
   * @return true if the transaction exists, false otherwise.
   */
  boolean existsByFlowIdAndCorrelationId(String flowId, String correlationId);

  /**
   * Save or update the transaction.
   *
   * @param transaction the transaction to save.
   * @return the saved transaction.
   */
  Transaction save(Transaction transaction);
}
