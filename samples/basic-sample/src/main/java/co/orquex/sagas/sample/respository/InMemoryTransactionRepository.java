package co.orquex.sagas.sample.respository;

import co.orquex.sagas.domain.api.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

  private final Map<Integer, Transaction> transactions = new ConcurrentHashMap<>();

  @Override
  public boolean existsByFlowIdAndCorrelationId(String flowId, String correlationId) {
    final var key = Objects.hash(flowId, correlationId);

    return Optional.ofNullable(transactions.get(key)).isPresent();
  }

  @Override
  public Transaction save(Transaction transaction) {
    final var key = Objects.hash(transaction.getFlowId(), transaction.getCorrelationId());
    transactions.put(key, transaction);
    return transaction;
  }
}
