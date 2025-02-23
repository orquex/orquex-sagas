package co.orquex.sagas.sample.cs.api.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfiguration {

  @Bean
  public NewTopic topic() {
    return TopicBuilder.name("coffee.shop.check-size.stage").partitions(3).replicas(1).build();
  }
}
