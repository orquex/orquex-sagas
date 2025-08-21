package co.orquex.sagas.domain.test;

import static co.orquex.sagas.domain.flow.FlowConfiguration.DEFAULT_ALL_OR_NOTHING;
import static co.orquex.sagas.domain.flow.FlowConfiguration.DEFAULT_TIMEOUT;
import static co.orquex.sagas.domain.test.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import co.orquex.sagas.domain.flow.Flow;
import co.orquex.sagas.domain.flow.FlowConfiguration;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.Stage;
import java.io.Serializable;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlowTest {

  static final String FLOW_ID = "flow-simple";
  static final String FLOW_NAME = "Simple Flow";
  static final String INITIAL_STAGE = "evaluation-impl";
  static final HashMap<String, Serializable> METADATA = new HashMap<>();
  static final FlowConfiguration CONFIGURATION = new FlowConfiguration();
  static final HashMap<String, Stage> stages = new HashMap<>();

  @BeforeAll
  static void beforeAll() {
    var stageEvaluationSimple = readValue("stage-evaluation-simple.json", Evaluation.class);
    stages.put(INITIAL_STAGE, stageEvaluationSimple);
  }

  @Test
  void shouldCreateSimpleFlowWithJackson() {
    final var flow = readValue("flow-simple.json", Flow.class);
    assertThat(flow).isNotNull();
    assertAll(
        () -> assertThat(flow.id()).isEqualTo(FLOW_ID),
        () -> assertThat(flow.name()).isEqualTo(FLOW_NAME),
        () -> assertThat(flow.initialStage()).isEqualTo(INITIAL_STAGE),
        () -> assertThat(flow.stages()).isNotEmpty().hasSize(2),
        () -> assertThat(flow.metadata()).isEmpty(),
        () ->
            assertThat(flow.configuration())
                .isNotNull()
                .matches(flowConfiguration -> flowConfiguration.timeout().equals(DEFAULT_TIMEOUT))
                .matches(
                    flowConfiguration ->
                        flowConfiguration.allOrNothing() == DEFAULT_ALL_OR_NOTHING));
  }

  @Test
  void shouldCreateFlowWithNonNullValues() {
    final var flow = new Flow(FLOW_ID, FLOW_NAME, INITIAL_STAGE, stages, METADATA, CONFIGURATION);
    assertAll(
        () -> assertThat(flow).isNotNull(),
        () -> assertThat(flow.id()).isEqualTo(FLOW_ID),
        () -> assertThat(flow.name()).isEqualTo(FLOW_NAME),
        () -> assertThat(flow.initialStage()).isEqualTo(INITIAL_STAGE),
        () -> assertThat(flow.stages()).hasSize(1),
        () -> assertThat(flow.metadata()).isEmpty(),
        () ->
            assertThat(flow.configuration())
                .isNotNull()
                .matches(flowConfiguration -> flowConfiguration.timeout().equals(DEFAULT_TIMEOUT))
                .matches(
                    flowConfiguration ->
                        flowConfiguration.allOrNothing() == DEFAULT_ALL_OR_NOTHING));
  }

  @Test
  void shouldSetNameAsIdIfNameIsNull() {
    final var flow = new Flow(FLOW_ID, null, INITIAL_STAGE, stages, METADATA, CONFIGURATION);
    assertThat(flow.name()).isEqualTo(FLOW_ID);
  }

  @Test
  void shouldThrowExceptionWithNullId() {
    assertThatThrownBy(
            () -> new Flow(null, FLOW_NAME, INITIAL_STAGE, stages, METADATA, CONFIGURATION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("flow's id required");
  }

  @Test
  void shouldThrowExceptionWithNullInitialStage() {
    assertThatThrownBy(() -> new Flow(FLOW_ID, FLOW_NAME, null, stages, METADATA, CONFIGURATION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("flow's initial stage required");
  }

  @Test
  void shouldCreateWithNonNullDefaultMetadata() {
    final var flow = new Flow(FLOW_ID, FLOW_NAME, INITIAL_STAGE, stages, null, CONFIGURATION);
    assertThat(flow.metadata()).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenInitialStageNotFoundInStages() {
    assertThatThrownBy(() -> new Flow(FLOW_ID, "invalid-initial-stage", stages))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "flow 'flow-simple' does not contains the initial stage 'invalid-initial-stage'");
  }

  @Test
  void shouldThrowExceptionWhenStageMapKeyDoesNotMatchStageId() {
    // Create a stage with a specific ID
    var stageEvaluationSimple = readValue("stage-evaluation-simple.json", Evaluation.class);
    var stagesWithMismatchedKey = new HashMap<String, Stage>();

    // Put the stage in the map with a different key than its ID
    stagesWithMismatchedKey.put("wrong-key", stageEvaluationSimple);

    assertThatThrownBy(
            () -> new Flow(
                FLOW_ID, FLOW_NAME, "wrong-key", stagesWithMismatchedKey, METADATA, CONFIGURATION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("flow 'Simple Flow': stage map key 'wrong-key' does not match stage id");
  }
}
