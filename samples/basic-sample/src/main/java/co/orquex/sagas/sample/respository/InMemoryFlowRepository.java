package co.orquex.sagas.sample.respository;

import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.flow.Flow;
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
    final var flow = getFlow("flow", objectMapper);
    final var flowSync = getFlow("flow-sync", objectMapper);
    this.flows = new ConcurrentHashMap<>();
    this.flows.put(flow.id(), flow);
    this.flows.put(flowSync.id(), flowSync);
  }

  private static Flow getFlow(String fileName, ObjectMapper objectMapper) throws IOException {
    final var file = ResourceUtils.getFile("classpath:data/%s.json".formatted(fileName));
    return objectMapper.readValue(file, Flow.class);
  }

  @Override
  public Optional<Flow> findById(String id) {
    return Optional.ofNullable(flows.get(id));
  }
}
