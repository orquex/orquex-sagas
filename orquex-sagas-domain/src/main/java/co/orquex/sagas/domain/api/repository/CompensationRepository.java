package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.transaction.Compensation;
import java.util.List;

/**
 * Repository interface that exposes the methods for managing the compensation execution stack of a
 * transaction.
 */
public interface CompensationRepository {

  /**
   * Find all created compensations by transaction ID.
   *
   * @param transactionId The transaction ID.
   * @return The list of compensations.
   */
  List<Compensation> findByTransactionId(String transactionId);

  /**
   * Create or update a compensation.
   *
   * @param compensation The compensation to be saved.
   * @return The saved compensation.˘
   */
  Compensation save(Compensation compensation);

  /**
   * Delete compensations by transaction ID.
   *
   * @param transactionId The transaction ID.
   */
  void deleteByTransactionId(String transactionId);
}
