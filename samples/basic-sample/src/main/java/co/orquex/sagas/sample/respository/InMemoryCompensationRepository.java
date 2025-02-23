package co.orquex.sagas.sample.respository;

import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.transaction.Compensation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCompensationRepository implements CompensationRepository {

  private final Map<String, List<Compensation>> compensations = new ConcurrentHashMap<>();

  @Override
  public List<Compensation> findByTransactionId(String transactionId) {
    return compensations.getOrDefault(transactionId, List.of());
  }

  @Override
  public Compensation save(Compensation compensation) {
    compensations
        .computeIfAbsent(compensation.transactionId(), k -> new ArrayList<>())
        .add(compensation);
    return compensation;
  }

  @Override
  public void deleteByTransactionId(String transactionId) {
    compensations.remove(transactionId);
  }
}
