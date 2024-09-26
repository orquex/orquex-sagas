package co.orquex.sagas.task.http.api;

import co.orquex.sagas.domain.exception.WorkflowException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Encapsulates metadata for an HTTP client activity implementation.
 *
 * <pre>
 * {
 *   "__client_provider": "",
 *   "__url": "",
 *   "__params": {
 *     "": ""
 *   },
 *   "__headers": {
 *    "": ""
 *   }
 * }
 * </pre>
 */
public record HttpActivityMetadata(
    @JsonProperty(METADATA_CLIENT_PROVIDER) String clientProvider,
    @JsonProperty(METADATA_URL) String url,
    @JsonProperty(METADATA_HEADERS) Map<String, String> headers,
    @JsonProperty(METADATA_PARAMS) Map<String, String> params) {

  public static final String METADATA_CLIENT_PROVIDER = "__client_provider";
  public static final String METADATA_URL = "__url";
  public static final String METADATA_HEADERS = "__headers";
  public static final String METADATA_PARAMS = "__params";

  public HttpActivityMetadata {
    if (clientProvider == null) {
      throw new WorkflowException("__client_provider is required");
    }
  }
}
