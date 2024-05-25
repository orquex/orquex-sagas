package co.orquex.sagas.sample.cs.api.respository;

import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.repository.FlowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

@Repository
public class InMemoryFlowRepository implements FlowRepository {

  private final ConcurrentMap<String, Flow> flows;

  public InMemoryFlowRepository(ObjectMapper objectMapper) throws IOException {
    final var file = ResourceUtils.getFile("classpath:data/flow.json");
    final var flow = objectMapper.readValue(file, Flow.class);
    this.flows = new ConcurrentHashMap<>();
    this.flows.put(flow.id(), flow);
  }

  @Override
  public Optional<Flow> findById(String id) {
    return Optional.ofNullable(flows.get(id));
  }
}
