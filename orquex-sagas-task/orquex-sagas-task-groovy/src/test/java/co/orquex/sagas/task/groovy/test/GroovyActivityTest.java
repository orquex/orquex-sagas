package co.orquex.sagas.task.groovy.test;

import static co.orquex.sagas.task.groovy.GroovyActivity.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.domain.task.TaskRequest;
import co.orquex.sagas.task.groovy.GroovyActivity;
import co.orquex.sagas.task.groovy.test.fixture.TestGroovyGlobalContext;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GroovyActivityTest {

  private static GroovyActivity groovyActivity;
  private static TestGroovyGlobalContext globalContext;

  @BeforeAll
  static void beforeAll() {
    globalContext = new TestGroovyGlobalContext();
    groovyActivity = new GroovyActivity(globalContext);
  }

  @Test
  void shouldExecuteSimpleScripts() {
    var expression =
        """
          if (payload.a == 1) {
            response.b = payload.a + 1
          }
        """;
    final Map<String, Serializable> metadata = Map.of(SCRIPT, toBase64(expression));
    final Map<String, Serializable> payload = Map.of("a", 1);
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);
    final var response = groovyActivity.execute(taskRequest);
    assertThat(response).hasSize(1).containsEntry("b", 2);
  }

  @Test
  void shouldAddVariableToGlobalContext() {
    var expression =
        """
          def a = [ 1, 2, 3 ]
          context.put("c", a)
          response.putAll(payload)
        """;
    final Map<String, Serializable> metadata = Map.of(SCRIPT, toBase64(expression));
    final Map<String, Serializable> payload = Map.of("a", 1);
    final var transactionId = UUID.randomUUID().toString();
    final var taskRequest = new TaskRequest(transactionId, metadata, payload);
    final var response = groovyActivity.execute(taskRequest);
    assertThat(response).hasSize(1).containsEntry("a", 1);
    assertThat(globalContext.get(transactionId).get("c")).isEqualTo(List.of(1, 2, 3));
  }

  private String toBase64(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes());
  }
}
