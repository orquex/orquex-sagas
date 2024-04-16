package co.orquex.sagas.domain.test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ExecutionRequestTest {

  public static final String FLOW_ID = UUID.randomUUID().toString();
  public static final String CORRELATION_ID = UUID.randomUUID().toString();
  public static final Map<String, Serializable> PAYLOAD = Map.of("payload-key", "payload-value");
  public static final Map<String, Serializable> METADATA = Map.of("metadata-key", "metadata-value");

  @Test
  public void shouldInitializeWithNonNullValues() {
    final var request = new ExecutionRequest(FLOW_ID, CORRELATION_ID, METADATA, PAYLOAD);
    assertThat(request).isNotNull();
    assertThat(request.flowId()).isEqualTo(FLOW_ID);
    assertThat(request.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(request.metadata())
        .isNotEmpty()
        .hasSize(1)
        .containsExactly(entry("metadata-key", "metadata-value"));
    assertThat(request.payload())
        .isNotEmpty()
        .hasSize(1)
        .containsExactly(entry("payload-key", "payload-value"));
  }

  @Test
  public void shouldThrowExceptionWithNullFlowId() {
    assertThatThrownBy(() -> new ExecutionRequest(null, CORRELATION_ID, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("execution request's flow id is required");
  }

  @Test
  public void shouldThrowExceptionWithNullCorrelationId() {
    assertThatThrownBy(() -> new ExecutionRequest(FLOW_ID, null, new HashMap<>(), new HashMap<>()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("execution request's correlation id is required");
  }

  @Test
  public void shouldCreateWithNonNullDefaultMetadataAndPayload() {
    ExecutionRequest request = new ExecutionRequest(FLOW_ID, CORRELATION_ID, null, null);
    assertThat(request).isNotNull();
    assertThat(request.flowId()).isEqualTo(FLOW_ID);
    assertThat(request.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(request.metadata()).isEmpty(); // Defaults to an empty map
    assertThat(request.payload()).isEmpty(); // Defaults to an empty map
  }
}
