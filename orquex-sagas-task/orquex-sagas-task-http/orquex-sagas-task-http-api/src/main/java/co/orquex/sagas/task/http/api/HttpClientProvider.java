package co.orquex.sagas.task.http.api;

import co.orquex.sagas.domain.api.registry.Registrable;

/** Provides a registrable HTTP client. */
public interface HttpClientProvider<T> extends Registrable {

  T getClient();
}
