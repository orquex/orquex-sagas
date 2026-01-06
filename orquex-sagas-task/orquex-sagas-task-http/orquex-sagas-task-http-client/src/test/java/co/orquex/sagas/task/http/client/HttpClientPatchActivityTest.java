package co.orquex.sagas.task.http.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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

@WireMockTest(httpPort = 7002)
class HttpClientPatchActivityTest {

  static final ObjectMapper objectMapper = new ObjectMapper();
  static Map<String, Serializable> metadata;
  static Map<String, Serializable> payload;
  static Serializable headers;
  static HttpClientPatchActivity httpClientPatchActivity;

  @BeforeAll
  static void beforeAll() {
    headers = new HashMap<>(Map.of("Content-Type", "application/json"));
    metadata =
        Map.of(
            "__url",
            "http://localhost:7002/users/1",
            "__client_provider",
            "basic-client",
            "__headers",
            headers);
    final var body = new HashMap<>();
    body.put("name", "John Doe");
    payload = Map.of("body", body);
    final var registry =
        HttpClientInMemoryClientProviderRegistry.of(List.of(new BasicHttpClientProvider()));
    httpClientPatchActivity = new HttpClientPatchActivity(registry, objectMapper);
  }

  @Test
  void shouldExecuteSuccessfulHttpPatch() {
    // Given
    stubFor(
        patch("/users/1")
            .withBasicAuth("name", "password")
            .willReturn(aResponse().withStatus(200).withBodyFile("user_response.json")));
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);

    // When
    final var result = httpClientPatchActivity.execute(taskRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(200);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }

  @Test
  void shouldExecuteFailedHttpPatch() {
    // Given
    stubFor(
        patch("/users/1").withBasicAuth("name", "password").willReturn(aResponse().withStatus(500)));
    final var taskRequest = new TaskRequest(UUID.randomUUID().toString(), metadata, payload);

    // When
    final var result = httpClientPatchActivity.execute(taskRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).extracting("code").isEqualTo(500);
    assertThat(result).extracting("body").isNotNull();
    assertThat(result).extracting("headers").isNotNull();
  }
}
