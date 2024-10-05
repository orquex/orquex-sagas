package co.orquex.sagas.task.http.api;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Abstract class that provides the basic implementation for the HTTP client task. */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractHttpClientTaskImplementation<C> implements TaskImplementation {

  protected final HttpClientProviderRegistry<C> registry;
  protected final ObjectMapper objectMapper;

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    final var activityMetadata = convertValue(taskRequest.metadata(), HttpActivityMetadata.class);
    final var activityPayload = convertValue(taskRequest.payload(), HttpActivityPayload.class);
    final var client = getHttpClient(activityMetadata.clientProvider());
    final var activityRequest =
        new HttpActivityRequest(taskRequest.transactionId(), activityMetadata, activityPayload);

    final var response = doRequest(client, activityRequest);

    return convertValue(response);
  }

  protected C getHttpClient(String clientProvider) {
    return registry
        .get(clientProvider)
        .orElseThrow(
            () -> new WorkflowException("HTTP Client provider '" + clientProvider + "' not found"))
        .getClient();
  }

  @NonNull
  protected Map<String, Serializable> convertValue(HttpActivityResponse httpActivityResponse) {
    return objectMapper.convertValue(httpActivityResponse, new TypeReference<>() {});
  }

  @NonNull
  protected <T> T convertValue(@NonNull Map<String, Serializable> object, @NonNull Class<T> clazz) {
    try {
      return objectMapper.convertValue(object, clazz);
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException(
          clazz.getSimpleName() + " deserialization error, check required fields");
    }
  }

  /**
   * Method that should be implemented by the concrete class to execute the HTTP request and return
   * the response.
   *
   * @param activityRequest The activity request object
   * @return a HttpActivityResponse instance.
   */
  protected abstract HttpActivityResponse doRequest(C client, HttpActivityRequest activityRequest);
}
