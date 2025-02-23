package co.orquex.sagas.task.http.api;

import co.orquex.sagas.domain.api.registry.Registry;

/** Provides a registry of HTTP client providers. */
public interface HttpClientProviderRegistry<C> extends Registry<HttpClientProvider<C>> {}
