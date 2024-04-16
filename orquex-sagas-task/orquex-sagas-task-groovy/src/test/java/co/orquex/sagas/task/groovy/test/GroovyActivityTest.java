package co.orquex.sagas.task.groovy.test;

import static co.orquex.sagas.domain.stage.Evaluation.EXPRESSION;
import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.task.groovy.GroovyActivity;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GroovyActivityTest {

  private static GroovyActivity groovyActivity;

  @BeforeAll
  static void beforeAll() {
    groovyActivity = new GroovyActivity();
  }

  @Test
  void shouldExecuteSimpleScripts() {
    var expression = """
        if (payload.a == 1) context.b = payload.a + 1
    """;
    final Map<String, Serializable> metadata = Map.of(EXPRESSION, expression);
    final Map<String, Serializable> payload = Map.of("a", 1);
    final var response = groovyActivity.execute(UUID.randomUUID().toString(), metadata, payload);
    assertThat(response).hasSize(1).containsEntry("b", 2);
  }
}
