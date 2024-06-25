package co.orquex.sagas.sample.config;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import lombok.RequiredArgsConstructor;
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
}
