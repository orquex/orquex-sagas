package co.orquex.sagas.sample.cs.api.controller;

import co.orquex.sagas.core.flow.AsyncWorkflowExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coffees")
@RequiredArgsConstructor
public class OrderController {

  private final AsyncWorkflowExecutor workflowExecutor;

  @PostMapping("/orders")
  public ResponseEntity<Map<String, String>> createOrder(@RequestBody ExecutionRequest request) {
    workflowExecutor.execute(request);
    return ResponseEntity.ok().body(Map.of("message", "Order placed successfully"));
  }
}
