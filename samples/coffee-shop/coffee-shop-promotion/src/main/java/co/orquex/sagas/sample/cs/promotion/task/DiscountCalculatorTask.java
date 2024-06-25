package co.orquex.sagas.sample.cs.promotion.task;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscountCalculatorTask implements TaskImplementation {

  private final ObjectMapper mapper;

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {
    final var hasSize = payload.containsKey("size");
    final var hasReferences = metadata.containsKey("references");
    if (hasReferences && hasSize) {
      final var size = payload.get("size").toString();
      final var references = mapper.convertValue(metadata.get("references"), References.class);
      final var discount =
          switch (size) {
            case "small" -> references.small;
            case "medium" -> references.medium;
            case "large" -> references.large;
            default -> 0;
          };
      return Map.of("size", size, "discount", discount);
    }
    throw new WorkflowException("Discount calculation failed");
  }

  public record References(int small, int medium, int large) {}

  @Override
  public String getName() {
    return "discount-calculator";
  }
}
