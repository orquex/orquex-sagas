package co.orquex.sagas.core.engine;


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

  /*private static Registry stagedRegistry;
  private static WorkflowEngine workflow;
  private static Flow simpleFlow;
  private static ExecutionRequest executionRequest;

  @BeforeAll
  static void beforeAll() {
    stagedRegistry = InMemoryStagedRegistry.newInstance();
    workflow = new WorkflowEngine(stagedRegistry);
    simpleFlow = readValue("flow-simple.json", Flow.class);
    executionRequest = new ExecutionRequest(simpleFlow.id(), UUID.randomUUID().toString());
  }

  @Test
  void shouldThrowExceptionWhenStagedNotFoundInRegistry() {
    Assertions.assertThatThrownBy(() -> workflow.start(simpleFlow, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("stage 'evaluation-impl' not registered");
  }

  @Test
  void shouldStartWorkflowExecution() {
    final StageExecutor<Evaluation> evaluationStaged =
        (stage, executionRequest) -> assertThat(stage.getId()).isNotNull();
    stagedRegistry.addStage(evaluationStaged);
    workflow.start(simpleFlow, executionRequest);
  }*/
}
