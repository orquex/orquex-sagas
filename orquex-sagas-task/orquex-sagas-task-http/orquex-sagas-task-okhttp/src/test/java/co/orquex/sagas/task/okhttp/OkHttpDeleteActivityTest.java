package co.orquex.sagas.task.okhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.domain.task.TaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 7001)
class OkHttpDeleteActivityTest {

  static final ObjectMapper objectMapper = new ObjectMapper();
  static Map<String, Serializable> metadata;
  static Map<String, Serializable> payload;
  static Serializable headers;
  static OkHttpDeleteActivity okHttpDeleteActivity;

  @BeforeAll
  static void beforeAll() {
    headers = new HashMap<>(Map.of("Content-Type", "application/json"));
    metadata =
        Map.of(
            "__url",
            "http://localhost:7001/users/1",
            "__client_provider",
            "basic-client",
            "__headers",
            headers);
    final var registry =
        OkHttpInMemoryClientProviderRegistry.of(List.of(new BasicOkHttpClientProvider()));
    okHttpDeleteActivity = new OkHttpDeleteActivity(registry, objectMapper);
  }

  @Test
  void shouldExecuteSuccessfulHttpDelete() {
    // Given
    stubFor(
        delete("/users/1")
            .withBasicAuth("name", "password")
            .willReturn(aResponse().withStatus(200)));
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);

    // When
    final var result = okHttpDeleteActivity.execute(taskRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(200);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }

  @Test
  void shouldExecuteFailedHttpDelete() {
    // Given
    stubFor(
        delete("/users/1")
            .withBasicAuth("name", "password")
            .willReturn(aResponse().withStatus(400)));
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);

    // When
    final var result = okHttpDeleteActivity.execute(taskRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(400);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }
}
