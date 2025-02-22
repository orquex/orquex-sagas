package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.context.DefaultGlobalContext;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.concurrent.ConcurrentHashMap;

import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure the {@link DefaultGlobalContext} implementation as a bean. */
@Configuration
public class SagasGlobalContextConfiguration {

  @ConditionalOnMissingBean(name = { "globalContext" })
  @Bean
  public GlobalContext globalContext() {
    final var globalContextMap = new ConcurrentHashMap<String, TransactionContext>();
    return new DefaultGlobalContext(globalContextMap);
  }
}
