package co.orquex.sagas.task.http.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the payload for an HTTP client activity implementation.
 *
 * <pre>
 * {
 *   "url": "",
 *   "params": {
 *     "": ""
 *   },
 *   "headers": {
 *     "": ""
 *   },
 *   "body": {}
 * }
 * </pre>
 */
public record HttpActivityPayload(
    @JsonProperty(PAYLOAD_URL) String url,
    @JsonProperty(PAYLOAD_PARAMS) Map<String, String> params,
    @JsonProperty(PAYLOAD_HEADERS) Map<String, String> headers,
    @JsonProperty(PAYLOAD_BODY) Map<String, Serializable> body) {

  public static final String PAYLOAD_URL = "url";
  public static final String PAYLOAD_PARAMS = "params";
  public static final String PAYLOAD_HEADERS = "headers";
  public static final String PAYLOAD_BODY = "body";

  public HttpActivityPayload() {
    this(null, null, null, null);
  }
}
