package co.orquex.sagas.task.okhttp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the payload for OkHttp activity implementation.
 *
 * <pre>
 * {
 *   "url": "https://example.com",
 *   "params": {
 *     "key": "value"
 *   },
 *   "headers": {
 *     "key": "value"
 *   },
 *   "body": {
 *     "key": "value"
 *   }
 * }
 * </pre>
 */
public record OkHttpActivityPayload(
    @JsonProperty("url") String url,
    @JsonProperty("params") Map<String, String> params,
    @JsonProperty("headers") Map<String, String> headers,
    @JsonProperty("body") Map<String, Serializable> body) {

  public OkHttpActivityPayload() {
    this(null, null, null, null);
  }
}
