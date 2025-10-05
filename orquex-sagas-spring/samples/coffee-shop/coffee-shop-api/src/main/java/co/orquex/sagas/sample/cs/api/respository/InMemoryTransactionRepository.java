package co.orquex.sagas.sample.cs.api.respository;

import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

  private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

  @Override
  public Optional<Transaction> findById(String id) {
    return Optional.ofNullable(transactions.get(id));
  }

  @Override
  public boolean existsByFlowIdAndCorrelationId(String flowId, String correlationId) {
    return transactions.values().stream()
        .anyMatch(
            transaction ->
                transaction.flowId().equals(flowId)
                    && transaction.correlationId().equals(correlationId));
  }

  @Override
  public Optional<Transaction> findByFlowIdAndCorrelationId(String flowId, String correlationId) {
    return transactions.values().stream()
        .filter(
            transaction ->
                transaction.flowId().equals(flowId)
                    && transaction.correlationId().equals(correlationId))
        .findFirst();
  }

  @Override
  public Transaction save(Transaction transaction) {
    transactions.put(transaction.transactionId(), transaction);
    return transaction;
  }
}
