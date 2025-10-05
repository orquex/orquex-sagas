package co.orquex.sagas.sample.cs.notification.config;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class NotificationConfiguration {

  @Bean
  public NewTopic topic() {
    return TopicBuilder.name("coffee.shop.stage.notification").partitions(3).replicas(1).build();
  }

  /** Add jackson support for the sagas to the current object mapper */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.modulesToInstall(new OrquexJacksonModule());
  }
}
