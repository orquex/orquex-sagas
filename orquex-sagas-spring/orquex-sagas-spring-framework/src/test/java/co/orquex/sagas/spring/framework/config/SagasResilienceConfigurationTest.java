package co.orquex.sagas.spring.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.core.resilience.CircuitBreakerState;
import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.resilience.impl.InMemoryCircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.impl.InMemoryRetryStateManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DisplayName("SagasResilienceConfiguration Tests")
class SagasResilienceConfigurationTest {

  @Nested
  @DisplayName("Default Bean Configuration")
  @ExtendWith(SpringExtension.class)
  @ContextConfiguration(classes = SagasResilienceConfiguration.class)
  class DefaultBeansTest {

    @Autowired private RetryStateManager retryStateManager;

    @Autowired private CircuitBreakerStateManager circuitBreakerStateManager;

    @Test
    @DisplayName("Should load default retry state manager when none exists")
    void shouldLoadDefaultRetryStateManager() {
      assertThat(retryStateManager).isNotNull().isInstanceOf(InMemoryRetryStateManager.class);
    }

    @Test
    @DisplayName("Should load default circuit breaker state manager when none exists")
    void shouldLoadDefaultCircuitBreakerStateManager() {
      assertThat(circuitBreakerStateManager)
          .isNotNull()
          .isInstanceOf(InMemoryCircuitBreakerStateManager.class);
    }
  }

  @Nested
  @DisplayName("Custom Bean Configuration")
  @ExtendWith(SpringExtension.class)
  @ContextConfiguration(
      classes = {SagasResilienceConfiguration.class, CustomBeansConfiguration.class})
  class CustomBeansTest {

    @Autowired private RetryStateManager retryStateManager;

    @Autowired private CircuitBreakerStateManager circuitBreakerStateManager;

    @Test
    @DisplayName("Should use custom retry state manager when provided")
    void shouldUseCustomRetryStateManager() {
      assertThat(retryStateManager).isNotNull().isInstanceOf(MockRetryStateManager.class);
    }

    @Test
    @DisplayName("Should use custom circuit breaker state manager when provided")
    void shouldUseCustomCircuitBreakerStateManager() {
      assertThat(circuitBreakerStateManager)
          .isNotNull()
          .isInstanceOf(MockCircuitBreakerStateManager.class);
    }
  }

  @Nested
  @DisplayName("Direct Bean Creation")
  class DirectBeanCreationTest {

    @Test
    @DisplayName("Should create default retry state manager instance")
    void shouldCreateDefaultRetryStateManagerInstance() {
      // Given
      SagasResilienceConfiguration config = new SagasResilienceConfiguration();

      // When
      RetryStateManager retryStateManager = config.defaultRetryStateManager();

      // Then
      assertThat(retryStateManager).isNotNull().isInstanceOf(InMemoryRetryStateManager.class);
    }

    @Test
    @DisplayName("Should create default circuit breaker state manager instance")
    void shouldCreateDefaultCircuitBreakerStateManagerInstance() {
      // Given
      SagasResilienceConfiguration config = new SagasResilienceConfiguration();

      // When
      CircuitBreakerStateManager circuitBreakerStateManager =
          config.defaultCircuitBreakerStateManager();

      // Then
      assertThat(circuitBreakerStateManager)
          .isNotNull()
          .isInstanceOf(InMemoryCircuitBreakerStateManager.class);
    }
  }

  @Configuration
  static class CustomBeansConfiguration {

    @Bean("retryStateManager")
    RetryStateManager customRetryStateManager() {
      return new MockRetryStateManager();
    }

    @Bean("circuitBreakerStateManager")
    CircuitBreakerStateManager customCircuitBreakerStateManager() {
      return new MockCircuitBreakerStateManager();
    }
  }

  static class MockRetryStateManager implements RetryStateManager {
    private final Map<String, Long> retryStates = new HashMap<>();

    @Override
    public void add(String state) {
      retryStates.put(state, 0L);
    }

    @Override
    public long value(String name) {
      return retryStates.getOrDefault(name, 0L);
    }

    @Override
    public long incrementAndGet(String name) {
      long newValue = retryStates.getOrDefault(name, 0L) + 1;
      retryStates.put(name, newValue);
      return newValue;
    }

    @Override
    public void reset(String name) {
      retryStates.remove(name);
    }
  }

  static class MockCircuitBreakerStateManager implements CircuitBreakerStateManager {
    private final Map<String, CircuitBreakerState.State> states = new HashMap<>();
    private final Map<String, Long> failureCounts = new HashMap<>();
    private final Map<String, Long> successCounts = new HashMap<>();
    private final Map<String, Instant> openedTimestamps = new HashMap<>();

    @Override
    public CircuitBreakerState.State getState(String name) {
      return states.getOrDefault(name, CircuitBreakerState.State.CLOSED);
    }

    @Override
    public void setState(String name, CircuitBreakerState.State state) {
      states.put(name, state);
    }

    @Override
    public long getFailureCount(String name) {
      return failureCounts.getOrDefault(name, 0L);
    }

    @Override
    public long getSuccessCount(String name) {
      return successCounts.getOrDefault(name, 0L);
    }

    @Override
    public long incrementFailureCount(String name) {
      long newValue = failureCounts.getOrDefault(name, 0L) + 1;
      failureCounts.put(name, newValue);
      return newValue;
    }

    @Override
    public long incrementSuccessCount(String name) {
      long newValue = successCounts.getOrDefault(name, 0L) + 1;
      successCounts.put(name, newValue);
      return newValue;
    }

    @Override
    public void resetFailureCount(String name) {
      failureCounts.put(name, 0L);
    }

    @Override
    public void resetSuccessCount(String name) {
      successCounts.put(name, 0L);
    }

    @Override
    public Instant getOpenedTimestamp(String name) {
      return openedTimestamps.get(name);
    }

    @Override
    public void setOpenedTimestamp(String name, Instant timestamp) {
      openedTimestamps.put(name, timestamp);
    }
  }
}
