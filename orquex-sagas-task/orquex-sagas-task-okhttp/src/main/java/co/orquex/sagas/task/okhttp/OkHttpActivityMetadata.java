package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.exception.WorkflowException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Encapsulates metadata for OkHttp activity implementation.
 *
 * <pre>
 * {
 *   "__client_provider": "okhttp",
 *   "__url": "https://example.com",
 *   "__params": {
 *     "key": "value"
 *   },
 *   "__headers": {
 *    "key": "value"
 *   }
 * }
 * </pre>
 */
public record OkHttpActivityMetadata(
    @JsonProperty("__client_provider") String clientProvider,
    @JsonProperty("__url") String url,
    @JsonProperty("__headers") Map<String, String> headers,
    @JsonProperty("__params") Map<String, String> params) {

  public OkHttpActivityMetadata {
    if (clientProvider == null) {
      throw new WorkflowException("OkHttpClient provider is required");
    }
  }
}
