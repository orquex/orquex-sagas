package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.api.registry.Registrable;
import okhttp3.OkHttpClient;

/**
 * Defines the methods to get an instance of {@link OkHttpClient}, this provider is shared across
 * the different tasks or HTTP clients implementations.
 */
public interface OkHttpClientProvider extends Registrable {

  /**
   * Gets an instance of {@link OkHttpClient}.
   *
   * @return an instance of {@link OkHttpClient}.
   */
  OkHttpClient getOkHttpClient();
}
