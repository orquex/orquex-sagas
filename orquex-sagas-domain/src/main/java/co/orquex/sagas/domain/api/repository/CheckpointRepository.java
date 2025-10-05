package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.transaction.Checkpoint;
import java.util.Optional;

public interface CheckpointRepository {

  Checkpoint save(Checkpoint checkpoint);

  Optional<Checkpoint> findByTransactionId(String transactionId);
}
