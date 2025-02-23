package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.task.http.api.AbstractHttpClientTaskImplementation;
import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpActivityResponse;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract class that provides the basic implementation for the OkHttp task.
 *
 * <p>It provides the basic implementation for the OkHttp task, including the execution of the task
 * and the conversion of the request and response objects.
 */
@Slf4j
abstract class OkHttpAbstractTaskImplementation
    extends AbstractHttpClientTaskImplementation<OkHttpClient> {

  protected OkHttpAbstractTaskImplementation(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  @Override
  protected HttpActivityResponse doRequest(
      OkHttpClient client, HttpActivityRequest activityRequest) {

    final var request = doRequest(activityRequest);

    try (final var response = client.newCall(request).execute()) {
      return new HttpActivityResponse(
          response.code(), getBody(response.body()), response.headers().toMultimap());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(e.getMessage());
    }
  }

  @NonNull
  protected Headers getHeaders(Map<String, String> headers) {
    if (headers == null) {
      return new Headers.Builder().build();
    }
    final var builder = new Headers.Builder();
    headers.forEach(builder::add);
    return builder.build();
  }

  @NonNull
  private Map<String, Serializable> getBody(@Nullable ResponseBody body) throws IOException {
    if (body != null) {
      final var bytes = body.bytes();
      if (bytes.length > 0) {
        return objectMapper.readValue(bytes, new TypeReference<>() {});
      }
    }
    return Collections.emptyMap();
  }

  protected String writeValueAsString(Map<String, Serializable> object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(e.getMessage());
    }
  }

  /**
   * Method that should be implemented by the concrete class to create the request object.
   *
   * @param activityRequest The activity request object
   * @return a OkHttp request object.
   */
  protected abstract Request doRequest(HttpActivityRequest activityRequest);
}
