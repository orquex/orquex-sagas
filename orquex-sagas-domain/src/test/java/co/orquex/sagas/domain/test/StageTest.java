package co.orquex.sagas.domain.test;

import static co.orquex.sagas.domain.stage.StageConfiguration.DEFAULT_IMPLEMENTATION;
import static co.orquex.sagas.domain.test.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import co.orquex.sagas.domain.stage.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StageTest {

  @Test
  void shouldCreateSimpleActivityWithJackson() {
    final var stage = readValue("stage-activity-simple.json", Stage.class);
    assertAll(
        () -> assertThat(stage).isNotNull(), () -> assertThat(stage).isInstanceOf(Activity.class));
    final var activity = (Activity) stage;
    assertAll(
        () -> assertThat(activity.getName()).isEqualTo("Simple Activity"),
        () -> assertThat(activity.getOutgoing()).isEqualTo("outgoing-test"),
        () -> assertThat(activity.getMetadata()).isNotEmpty().hasSize(1),
        () -> assertThat(activity.getActivityTasks()).isNotEmpty().hasSize(1),
        () -> assertThat(activity.getConfiguration()).isNotNull());
    final var configuration = stage.getConfiguration();
    assertThat(configuration.implementation()).isEqualTo(DEFAULT_IMPLEMENTATION);
  }

  @Test
  void shouldThrowExceptionWhenStageTypeNullWithJackson() {
    assertThrowsExactly(
        RuntimeException.class, () -> readValue("stage-activity-no-type.json", Stage.class));
  }

  @Test
  void shouldSetMetadataEmptyWhenNull() {
    final var activityTasks = List.of(new ActivityTask("simple-task"));
    final var activity =
        new Activity(null, null, null, null, activityTasks, false, "outgoing", null);
    assertThat(activity.getMetadata()).isEmpty();
  }

  @Test
  void shouldSetActivityParallelTrue() {
    final var activityTasks = List.of(new ActivityTask("simple-task"));
    final var activity =
        new Activity(null, "single", null, null, activityTasks, true, "outgoing", null);
    assertThat(activity.isParallel()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenActivityTaskNull() {
    assertThatThrownBy(() -> new Activity(null, "empty-tasks", null, null, null, false, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("activity 'empty-tasks' does not contains tasks");
  }

  @Test
  void shouldCreateEvaluationWithJackson() {
    final var stage = readValue("stage-evaluation-simple.json", Stage.class);
    assertAll(
        () -> assertThat(stage).isNotNull(),
        () -> assertThat(stage).isInstanceOf(Evaluation.class));
    final var evaluation = (Evaluation) stage;
    assertAll(
        () -> assertThat(evaluation.getName()).isEqualTo("Simple Evaluation"),
        () -> assertThat(evaluation.getType()).isEqualTo("evaluation"),
        () -> assertThat(evaluation.getMetadata()).isNotEmpty().hasSize(1),
        () -> assertThat(evaluation.getDefaultOutgoing()).isEqualTo("outgoing-test-1"),
        () -> assertThat(evaluation.getConditions()).isNotEmpty().hasSize(2));
  }

  @Test
  void shouldThrowExceptionWhenEvaluationConditionsNull() {
    final var evaluationTask = new EvaluationTask("task-evaluator");
    assertThatThrownBy(
            () -> new Evaluation(null, null, null, null, evaluationTask, null, "default-outgoing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evaluation's conditions required");
  }

  @Test
  void shouldThrowExceptionWhenEvaluationConditionsEmpty() {
    final var evaluationTask = new EvaluationTask("task-evaluator");
    final List<Condition> conditions = Collections.emptyList();
    assertThatThrownBy(
            () ->
                new Evaluation(
                    null, null, null, null, evaluationTask, conditions, "default-outgoing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evaluation's conditions required");
  }

  @ParameterizedTest(name = "#{index} - Run test with args=''{0}''")
  @NullSource
  @ValueSource(strings = {"", " "})
  void shouldThrowExceptionWhenEvaluationDefaultOutgoingNullOrEmpty(String defaultOutgoing) {
    final var evaluationTask = new EvaluationTask("task-evaluator");
    assertThatThrownBy(
            () -> new Evaluation(null, null, null, null, evaluationTask, null, defaultOutgoing))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evaluation's conditions required");
  }
}
