package co.orquex.sagas.sample.controller;

import co.orquex.sagas.core.flow.WorkflowExecutor;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/name")
@RequiredArgsConstructor
public class FlowController {

    private final WorkflowExecutor workflowExecutor;

    @PostMapping("/customizer")
    public ResponseEntity<Void> upperCase(@RequestBody ExecutionRequest request) {
        workflowExecutor.execute(request);
        return ResponseEntity.ok().build();
    }

}

