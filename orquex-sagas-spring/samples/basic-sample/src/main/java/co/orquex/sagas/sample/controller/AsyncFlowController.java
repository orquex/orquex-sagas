package co.orquex.sagas.sample.controller;

import co.orquex.sagas.core.flow.AsyncWorkflowExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/async")
@RequiredArgsConstructor
public class AsyncFlowController {

  private final AsyncWorkflowExecutor workflowExecutor;

  @PostMapping
  public ResponseEntity<Void> upperCase(@RequestBody ExecutionRequest request) {
    final var transactionId = workflowExecutor.execute(request);
    return ResponseEntity.ok().header("Transaction-Id", transactionId).build();
  }
}
