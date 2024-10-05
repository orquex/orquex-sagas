package co.orquex.sagas.task.groovy.test;

import static co.orquex.sagas.task.groovy.GroovyActivity.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.domain.task.TaskRequest;
import co.orquex.sagas.task.groovy.GroovyActivity;
import java.io.Serializable;
import java.util.Base64;
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
    var expression =
        """
          if (payload.a == 1) {
            context.b = payload.a + 1
          }
        """;
    final Map<String, Serializable> metadata = Map.of(SCRIPT, toBase64(expression));
    final Map<String, Serializable> payload = Map.of("a", 1);
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);
    final var response = groovyActivity.execute(taskRequest);
    assertThat(response).hasSize(1).containsEntry("b", 2);
  }

  private String toBase64(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes());
  }
}
