package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.flow.Flow;
import java.util.Optional;

/** Repository for managing flows. */
public interface FlowRepository {

  /**
   * Find a flow by its ID.
   *
   * @param id the flow ID.
   * @return the flow if found or empty otherwise.
   */
  Optional<Flow> findById(String id);
}
