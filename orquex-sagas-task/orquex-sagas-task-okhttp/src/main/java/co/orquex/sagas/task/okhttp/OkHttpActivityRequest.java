package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.exception.WorkflowException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import okhttp3.HttpUrl;

/**
 * Represents an HTTP request to be executed by the OkHttp activity implementation.
 *
 * @param transactionId The transaction internal workflow ID.
 * @param metadata The metadata of the request.
 * @param payload The payload of the request.
 */
record OkHttpActivityRequest(
    String transactionId, OkHttpActivityMetadata metadata, OkHttpActivityPayload payload) {

  public OkHttpActivityRequest {
    if (payload == null) {
      payload = new OkHttpActivityPayload();
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
    HttpUrl url;
    if (payload.url() != null) {
      url = HttpUrl.parse(payload.url());
    } else {
      url = HttpUrl.parse(metadata.url());
    }

    if (url == null) {
      throw new WorkflowException("URL is required");
    }

    var urlBuilder = url.newBuilder();
    setQueryParams(urlBuilder, merge(metadata.params(), payload.params()));

    return urlBuilder.build().toString();
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

  private void setQueryParams(HttpUrl.Builder urlBuilder, Map<String, String> queryParams) {
    if (queryParams != null) {
      queryParams.forEach(urlBuilder::addQueryParameter);
    }
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
