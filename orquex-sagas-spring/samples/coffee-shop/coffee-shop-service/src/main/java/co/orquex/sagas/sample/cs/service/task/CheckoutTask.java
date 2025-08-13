package co.orquex.sagas.sample.cs.service.task;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckoutTask implements TaskImplementation {

  private final ObjectMapper mapper;

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    final var metadata = taskRequest.metadata();
    final var payload = taskRequest.payload();
    final var hasProducts = metadata.containsKey("products");
    final var hasPayload = payload != null && !payload.isEmpty();
    if (hasProducts && hasPayload) {
      final var products =
          mapper.convertValue(metadata.get("products"), new TypeReference<List<Product>>() {});
      final var request = mapper.convertValue(payload, ServiceRequest.class);
      final var product =
          products.stream()
              .filter(p -> p.size.equals(request.size))
              .findFirst()
              .orElseThrow(
                  () -> new WorkflowException(String.format("Size '%s' not found", request.size)));
      var total = product.price;
      if (request.discount > 0) {
        total = product.price - (product.price * request.discount / 100.0);
      }
      return Map.of("order", new Order(product.size, product.price, request.discount, total));
    }
    throw new WorkflowException("Service checkout failed");
  }

  public record ServiceRequest(String size, int discount) implements Serializable {}

  public record Product(String size, double price) {}

  public record Order(String size, double price, int discount, double total)
      implements Serializable {}

  @Override
  public String getKey() {
    return "service-checkout";
  }
}
