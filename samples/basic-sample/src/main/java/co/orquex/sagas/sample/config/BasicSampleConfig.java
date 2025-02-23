package co.orquex.sagas.sample.config;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BasicSampleConfig {

  /** Add jackson support for the sagas to the current object mapper */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.modulesToInstall(new OrquexJacksonModule());
  }

  @Bean
  public HttpClientProvider<OkHttpClient> okHttpClientBasicProvider() {
    return new HttpClientProvider<>() {
      @Override
      public OkHttpClient getClient() {
        final var dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(10); // Total concurrent requests
        dispatcher.setMaxRequestsPerHost(2); // Per host

        return new OkHttpClient.Builder()
            .readTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(30))
            .retryOnConnectionFailure(false)
            .dispatcher(dispatcher)
            .build();
      }

      @Override
      public String getKey() {
        return "basic-provider";
      }
    };
  }
}
