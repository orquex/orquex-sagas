package co.orquex.sagas.task.http.client;

import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.task.http.api.AbstractHttpClientTaskImplementation;
import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpActivityResponse;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class HttpClientAbstractTaskImplementation
    extends AbstractHttpClientTaskImplementation<HttpClient> {

    protected HttpClientAbstractTaskImplementation(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  @Override
  protected HttpActivityResponse doRequest(HttpClient client, HttpActivityRequest activityRequest) {

    final var request = doRequest(activityRequest);

    try {
      return client.execute(
          request,
          response ->
              new HttpActivityResponse(
                  response.getCode(),
                  getBody(response.getEntity()),
                  getHeaders(response.getHeaders())));
    } catch (IOException e) {
      throw new WorkflowException("An error occurred while executing the HTTP request", e);
    }
  }

  @NonNull
  private Map<String, List<String>> getHeaders(Header[] headers) {
    if (headers == null || headers.length == 0) {
      return Collections.emptyMap();
    }
    return Arrays.stream(headers)
        .collect(
            Collectors.groupingBy(
                Header::getName, Collectors.mapping(Header::getValue, Collectors.toList())));
  }

  @NonNull
  protected Header[] getHeaders(Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return new Header[0];
    }
    return headers.entrySet().stream()
        .map(entry -> new BasicHeader(entry.getKey(), entry.getValue()))
        .toArray(Header[]::new);
  }

  @NonNull
  private Map<String, Serializable> getBody(@Nullable HttpEntity entity) throws IOException {
    if (entity != null) {
      final var bytes = EntityUtils.toByteArray(entity);
      if (bytes.length > 0) {
        try {
          return objectMapper.readValue(bytes, new TypeReference<>() {});
        } catch (IOException e) {
          throw new WorkflowException("An error occurred while deserializing the request body", e);
        }
      }
    }
    return Collections.emptyMap();
  }

  protected String writeValueAsString(Map<String, Serializable> object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (IOException e) {
      throw new WorkflowException("An error occurred while serializing the request body", e);
    }
  }

  /**
   * Method that should be implemented by the concrete class to create the request object.
   *
   * @param activityRequest The activity request object
   * @return a {@link HttpUriRequestBase} instance.
   */
  protected abstract HttpUriRequestBase doRequest(HttpActivityRequest activityRequest);
}
