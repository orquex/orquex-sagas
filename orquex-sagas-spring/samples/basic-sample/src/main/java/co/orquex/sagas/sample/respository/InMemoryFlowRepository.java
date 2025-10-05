package co.orquex.sagas.sample.respository;

import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.flow.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFlowRepository implements FlowRepository {

  private final ConcurrentMap<String, Flow> flows;

  public InMemoryFlowRepository(ObjectMapper objectMapper) {
    this.flows = new ConcurrentHashMap<>(loadFlows(objectMapper));
  }

  private Map<String, Flow> loadFlows(ObjectMapper objectMapper) {
    try {
      final var flowMap = new HashMap<String, Flow>();
      // Get the data directory from classpath
      final var resource = new ClassPathResource("data/flow");
      final var dataDir = Paths.get(resource.getURI());

      // Find all JSON files in the directory and load them
      try (final var paths = Files.walk(dataDir)) {
        paths
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().toLowerCase().endsWith(".json"))
            .forEach(
                path -> {
                  try {
                    final var flow = objectMapper.readValue(path.toFile(), Flow.class);
                    flowMap.put(flow.id(), flow);
                  } catch (IOException e) {
                    throw new IllegalStateException("Failed to load flow from file: " + path, e);
                  }
                });
      }
      return flowMap;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load flows from data directory", e);
    }
  }

  @Override
  public Optional<Flow> findById(String id) {
    return Optional.ofNullable(flows.get(id));
  }
}
