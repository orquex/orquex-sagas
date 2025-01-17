package co.orquex.sagas.sample.respository;

import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.flow.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

@Repository
public class InMemoryFlowRepository implements FlowRepository {

  private final ConcurrentMap<String, Flow> flows;

  public InMemoryFlowRepository(ObjectMapper objectMapper) {
    this.flows = new ConcurrentHashMap<>();
    Stream.of("flow", "flow-sync", "flow-compensation")
        .map(fileName -> getFlow(fileName, objectMapper))
        .forEach(flow -> flows.put(flow.id(), flow));
  }

  private static Flow getFlow(String fileName, ObjectMapper objectMapper) {
    try {
      final var file = ResourceUtils.getFile("classpath:data/%s.json".formatted(fileName));
      return objectMapper.readValue(file, Flow.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Optional<Flow> findById(String id) {
    return Optional.ofNullable(flows.get(id));
  }
}
