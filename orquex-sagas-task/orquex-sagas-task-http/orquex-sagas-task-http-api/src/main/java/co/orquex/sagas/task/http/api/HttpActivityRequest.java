package co.orquex.sagas.task.http.api;

import co.orquex.sagas.domain.exception.WorkflowException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an HTTP request to be executed by the client activity implementation.
 *
 * @param transactionId The transaction internal workflow ID.
 * @param metadata The metadata of the request.
 * @param payload The payload of the request.
 */
public record HttpActivityRequest(
    String transactionId, HttpActivityMetadata metadata, HttpActivityPayload payload) {

  public HttpActivityRequest {
    if (payload == null) {
      payload = new HttpActivityPayload();
    }
  }

  /**
   * Returns the URL to be used in the HTTP request by merging the metadata and payload URL fields
   * and setting the query parameters accordingly if they're present in the metadata or payload of
   * the request.
   *
   * @return The formatted URL to be used in the HTTP request.
   */
  public String url() {

    StringBuilder urlBuilder = new StringBuilder();
    if (payload.url() != null) {
      urlBuilder.append(payload.url());
    } else {
      urlBuilder.append(metadata.url());
    }

    if (urlBuilder.isEmpty()) {
      throw new WorkflowException("URL is required");
    }
    setQueryParams(urlBuilder, merge(metadata.params(), payload.params()));

    return urlBuilder.toString();
  }

  private void setQueryParams(StringBuilder urlBuilder, Map<String, String> queryParams) {
    if (queryParams != null) {
      urlBuilder.append("?");
      final var params =
              queryParams.entrySet().stream()
                      .map(entry -> entry.getKey() + "=" + entry.getValue())
                      .collect(Collectors.joining("&"));
      urlBuilder.append(params);
    }
  }

  /**
   * Returns the body of the request given in the payload.
   *
   * @return The body of the request.
   */
  public Map<String, Serializable> body() {
    return payload.body();
  }

  /**
   * Returns the headers of the request by merging the metadata and payload headers.
   *
   * @return The headers of the request.
   */
  public Map<String, String> headers() {
    return merge(metadata.headers(), payload.headers());
  }

  private Map<String, String> merge(Map<String, String> metadata, Map<String, String> payload) {
    if (metadata == null && payload == null) {
      return Collections.emptyMap();
    }

    if (metadata == null) {
      return payload;
    }

    if (payload == null) {
      return metadata;
    }

    final Map<String, String> merged = new HashMap<>(metadata);
    merged.putAll(payload);

    return merged;
  }
}
