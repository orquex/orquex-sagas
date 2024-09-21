package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.exception.WorkflowException;
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
abstract class OkHttpAbstractTaskImplementation implements TaskImplementation {

  protected final Registry<OkHttpClientProvider> registry;
  protected final ObjectMapper objectMapper;

  protected OkHttpAbstractTaskImplementation(Registry<OkHttpClientProvider> registry) {
    this.registry = registry;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {
    final var activityMetadata = convertValue(metadata, OkHttpActivityMetadata.class);
    final var activityPayload = convertValue(payload, OkHttpActivityPayload.class);
    final var client = getOkHttpClient(activityMetadata.clientProvider());

    final var activityRequest =
        new OkHttpActivityRequest(transactionId, activityMetadata, activityPayload);

    final var request = doRequest(activityRequest);

    try (final var response = client.newCall(request).execute()) {
      final var okHttpActivityResponse =
          new OkHttpActivityResponse(
              response.code(), getBody(response.body()), response.headers().toMultimap());
      return convertValue(okHttpActivityResponse);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(e.getMessage());
    }
  }

  protected OkHttpClient getOkHttpClient(String clientProvider) {
    return registry
        .get(clientProvider)
        .orElseThrow(() -> new WorkflowException("OkHttpClient provider not found"))
        .getOkHttpClient();
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

  @NonNull
  protected Map<String, Serializable> convertValue(@NonNull Object object) {
    try {
      return objectMapper.convertValue(object, new TypeReference<>() {});
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(e.getMessage());
    }
  }

  @NonNull
  protected String convertValue(@NonNull Map<String, Serializable> object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(e.getMessage());
    }
  }

  @NonNull
  protected <T> T convertValue(@NonNull Map<String, Serializable> object, @NonNull Class<T> clazz) {
    try {
      return objectMapper.convertValue(object, clazz);
    } catch (IllegalArgumentException e) {
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
  protected abstract Request doRequest(OkHttpActivityRequest activityRequest);
}
