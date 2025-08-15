package co.orquex.sagas.spring.framework.config;

import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.resilience.impl.InMemoryCircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.impl.InMemoryRetryStateManager;
import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default resilience state managers for sagas. */
@Slf4j
@Configuration
public class SagasResilienceConfiguration {

  /** Default retry state manager. */
  @Bean
  @ConditionalOnMissingBean(name = {"defaultRetryStateManager", "retryStateManager"})
  RetryStateManager defaultRetryStateManager() {
    return new InMemoryRetryStateManager();
  }

  /** Default circuit breaker state manager. */
  @Bean
  @ConditionalOnMissingBean(
      name = {"defaultCircuitBreakerStateManager", "circuitBreakerStateManager"})
  CircuitBreakerStateManager defaultCircuitBreakerStateManager() {
    return new InMemoryCircuitBreakerStateManager();
  }
}
