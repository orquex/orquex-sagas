package co.orquex.sagas.domain.repository;

import co.orquex.sagas.domain.flow.Flow;
import java.util.Optional;

public interface FlowRepository {

  Optional<Flow> findById(String id);
}
