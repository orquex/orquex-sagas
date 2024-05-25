package co.orquex.sagas.sample.cs.api.respository;

import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Transaction;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

  private final Map<String, Map<String, Transaction>> transactions = new ConcurrentHashMap<>();

  @Override
  public boolean existByFlowIdAndCorrelationId(String flowId, String correlationId) {
    return Optional.ofNullable(transactions.get(flowId))
        .map(txMap -> txMap.containsKey(correlationId))
        .orElse(false);
  }

  @Override
  public Transaction save(Transaction transaction) {
    String flowId = transaction.getFlowId();
    String correlationId = transaction.getCorrelationId();
    transactions.put(flowId, Map.of(correlationId, transaction));
    return transaction;
  }
}
