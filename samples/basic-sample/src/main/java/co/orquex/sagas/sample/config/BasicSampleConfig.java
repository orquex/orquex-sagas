package co.orquex.sagas.sample.config;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // For managing async event handling
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
        return new OkHttpClient.Builder().build();
      }

      @Override
      public String getKey() {
        return "basic-provider";
      }
    };
  }
}
