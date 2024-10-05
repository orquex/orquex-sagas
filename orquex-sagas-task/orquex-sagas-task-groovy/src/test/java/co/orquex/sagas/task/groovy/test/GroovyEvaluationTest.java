package co.orquex.sagas.task.groovy.test;

import static co.orquex.sagas.task.groovy.GroovyEvaluation.EXPRESSION;
import static co.orquex.sagas.task.groovy.GroovyEvaluation.RESULT;
import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import co.orquex.sagas.task.groovy.GroovyEvaluation;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GroovyEvaluationTest {

  private static GroovyEvaluation groovyEvaluation;

  @BeforeAll
  static void beforeAll() {
    groovyEvaluation = new GroovyEvaluation();
  }

  @Test
  void shouldEvaluateSimpleExpressions() {
    final Map<String, Serializable> metadata = Map.of(EXPRESSION, "payload.a == payload.b");
    final Map<String, Serializable> payload = Map.of("a", 1, "b", 1);
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);
    final var response = groovyEvaluation.execute(taskRequest);
    assertThat(response).hasSize(1).containsEntry(RESULT, true);
  }

  @Test
  void shouldThrowExceptionWhenExpressionNotBoolean() {
    final Map<String, Serializable> metadata = Map.of(EXPRESSION, "println \"hello\"");
    final Map<String, Serializable> payload = Collections.emptyMap();
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);
    Assertions.assertThatThrownBy(() -> groovyEvaluation.execute(taskRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("expression is not boolean");
  }

  @Test
  void shouldThrowExceptionWhenSyntaxError() {
    final Map<String, Serializable> metadata = Map.of(EXPRESSION, "foo");
    final Map<String, Serializable> payload = Collections.emptyMap();
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);
    Assertions.assertThatThrownBy(() -> groovyEvaluation.execute(taskRequest))
        .isInstanceOf(WorkflowException.class);
  }
}
