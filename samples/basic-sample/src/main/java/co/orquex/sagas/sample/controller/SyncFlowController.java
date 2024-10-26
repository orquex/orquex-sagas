package co.orquex.sagas.sample.controller;

import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/name/sync")
@RequiredArgsConstructor
public class SyncFlowController {

  private final WorkflowExecutor workflowExecutor;

  @PostMapping("/customizer")
  public ResponseEntity<Map<String, Serializable>> upperCase(
      @RequestBody ExecutionRequest request) {
    final var response = workflowExecutor.execute(request);
    return ResponseEntity.ok().body(response);
  }
}
