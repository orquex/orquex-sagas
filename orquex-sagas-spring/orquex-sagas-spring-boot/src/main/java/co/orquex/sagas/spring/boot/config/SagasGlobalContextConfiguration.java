package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.core.context.DefaultGlobalContext;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagasGlobalContextConfiguration {

  @ConditionalOnMissingBean(GlobalContext.class)
  @Bean
  public GlobalContext globalContext() {
    final var globalContextMap = new ConcurrentHashMap<String, TransactionContext>();
    return new DefaultGlobalContext(globalContextMap);
  }
}
