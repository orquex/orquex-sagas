package co.orquex.sagas.task.okhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

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
class OkHttpGetActivityTest {

  static ObjectMapper objectMapper = new ObjectMapper();
  static Map<String, Serializable> metadata;
  static Map<String, Serializable> payload;
  static Serializable headers;
  static OkHttpGetActivity okHttpGetActivity;

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
    final var registry = OkHttpInMemoryClientProviderRegistry.of(List.of(new BasicOkHttpClientProvider()));
    okHttpGetActivity = new OkHttpGetActivity(registry, objectMapper);
  }

  @Test
  void shouldExecuteSuccessfulHttpGet() {
    // Given
    stubFor(
        get("/users/1")
            .withBasicAuth("name", "password")
            .willReturn(aResponse().withStatus(200).withBodyFile("user_info.json")));

    // When
    final var result = okHttpGetActivity.execute(UUID.randomUUID().toString(), metadata, payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(200);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }

  @Test
  void shouldExecuteFailedHttpGet() {
    // Given
    stubFor(
        get("/users/1").withBasicAuth("name", "password").willReturn(aResponse().withStatus(400)));

    // When
    final var result = okHttpGetActivity.execute(UUID.randomUUID().toString(), metadata, payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(400);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }
}
