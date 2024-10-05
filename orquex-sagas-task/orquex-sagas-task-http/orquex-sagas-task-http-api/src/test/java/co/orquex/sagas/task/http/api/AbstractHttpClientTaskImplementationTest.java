package co.orquex.sagas.task.http.api;

import static co.orquex.sagas.task.http.api.HttpActivityMetadata.*;
import static co.orquex.sagas.task.http.api.HttpActivityPayload.PAYLOAD_BODY;
import static co.orquex.sagas.task.http.api.TestHttpClientProvider.HTTP_CLIENT_PROVIDER_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import co.orquex.sagas.task.http.api.TestHttpClient.TestHttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractHttpClientTaskImplementationTest {

  static String transactionId = UUID.randomUUID().toString();
  ObjectMapper objectMapper = new ObjectMapper();
  HttpClientProviderRegistry<TestHttpClient> registry;
  @Mock private TestHttpResponse httpResponse;

  @BeforeEach
  void setUp() {
    registry = TestHttpClientProviderRegistry.getInstance();
    registry.add(new TestHttpClientProvider(httpResponse));
  }

  @Test
  void testHttpClientTaskImplementationExecution() {
    final var postTask = new TestHttpPostTask(registry, objectMapper);
    final Map<String, Serializable> metadata = getMetadata(HTTP_CLIENT_PROVIDER_TEST);
    final Map<String, Serializable> payload = getPayload();
    final var taskRequest = new TaskRequest(transactionId, metadata, payload);
    final var response = postTask.execute(taskRequest);
    assertThat(response).isNotEmpty().hasSize(3);
  }

  @Test
  void shouldThrowExceptionWhenInvalidMetadata() {
    final var postTask = new TestHttpPostTask(registry, objectMapper);
    final Map<String, Serializable> metadata = new HashMap<>();
    final Map<String, Serializable> payload = getPayload();
    final var taskRequest = new TaskRequest(transactionId, metadata, payload);
    assertThatThrownBy(() -> postTask.execute(taskRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("HttpActivityMetadata deserialization error, check required fields");
  }

  @Test
  void shouldThrowExceptionWhenClientProviderNotFound() {
    final var postTask = new TestHttpPostTask(registry, objectMapper);
    final Map<String, Serializable> metadata = getMetadata("invalid-client-provider");
    final Map<String, Serializable> payload = getPayload();
    final var taskRequest = new TaskRequest(transactionId, metadata, payload);
    assertThatThrownBy(() -> postTask.execute(taskRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("HTTP Client provider 'invalid-client-provider' not found");
  }

  private static Map<String, Serializable> getPayload() {
    final Map<String, Serializable> payload = new HashMap<>();
    final var body = new HashMap<>();
    payload.put(PAYLOAD_BODY, body);
    return payload;
  }

  private static Map<String, Serializable> getMetadata(final String clientProvider) {
    final var headers = new HashMap<String, Serializable>();
    headers.put("Content-Type", "application/json");
    final var params = new HashMap<String, Serializable>();
    params.put("key", "value");
    params.put("foo", "bar");
    final Map<String, Serializable> metadata = new HashMap<>();
    metadata.put(METADATA_CLIENT_PROVIDER, clientProvider);
    metadata.put(METADATA_URL, "https://example.com");
    metadata.put(METADATA_HEADERS, headers);
    metadata.put(METADATA_PARAMS, params);
    return metadata;
  }
}
