package co.orquex.sagas.core.resilience;

import co.orquex.sagas.core.resilience.impl.CircuitBreakerImpl;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Circuit Breaker resilience pattern interface for fault tolerance and failure recovery.
 *
 * <p>The Circuit Breaker pattern provides a safety mechanism to prevent cascading failures in
 * distributed systems by monitoring service calls and automatically stopping requests to failing
 * services, allowing them time to recover.
 *
 * <h3>Core Concept:</h3>
 *
 * <p>Just like an electrical circuit breaker that protects electrical circuits from damage caused
 * by overload or short circuit, this pattern protects services from being overwhelmed by requests
 * when they are already failing.
 *
 * <h3>Circuit States:</h3>
 *
 * <ul>
 *   <li><strong>CLOSED:</strong> Normal operation - all calls pass through and are monitored for
 *       failures
 *   <li><strong>HALF_OPEN:</strong> Recovery testing - limited calls allowed to test service health
 *   <li><strong>OPEN:</strong> Failure protection - calls are blocked or redirected to fallback
 * </ul>
 *
 * <h3>State Transitions:</h3>
 *
 * <ul>
 *   <li><strong>CLOSED → OPEN:</strong> When failure threshold is exceeded
 *   <li><strong>OPEN → HALF_OPEN:</strong> When break duration expires (automatic recovery attempt)
 *   <li><strong>HALF_OPEN → CLOSED:</strong> When success threshold is met (service recovered)
 *   <li><strong>HALF_OPEN → OPEN:</strong> When any failure occurs during testing (service still
 *       failing)
 * </ul>
 *
 * <h3>Key Benefits:</h3>
 *
 * <ul>
 *   <li><strong>Prevents cascading failures</strong> by stopping calls to failing services
 *   <li><strong>Provides fast failure</strong> without waiting for timeouts
 *   <li><strong>Allows service recovery</strong> by reducing load during outages
 *   <li><strong>Improves system resilience</strong> and overall reliability
 *   <li><strong>Enables graceful degradation</strong> through fallback mechanisms
 * </ul>
 *
 * @see CircuitBreakerConfiguration
 * @see CircuitBreakerStateManager
 * @see CircuitBreakerFallback
 */
public interface CircuitBreaker {

  /**
   * Creates a new CircuitBreaker instance with the provided configuration.
   *
   * <p>This factory method provides the recommended way to create circuit breaker instances. It
   * encapsulates the implementation details and ensures proper initialization.
   *
   * @param configuration the circuit breaker configuration containing thresholds, timeouts, and
   *     other behavioral parameters. Must not be null.
   * @return a new CircuitBreaker instance ready for use
   * @throws IllegalArgumentException if configuration is null
   * @see CircuitBreakerConfiguration
   */
  static CircuitBreaker of(CircuitBreakerConfiguration configuration) {
    return new CircuitBreakerImpl(configuration);
  }

  /**
   * Executes the provided supplier function with circuit breaker protection.
   *
   * <p>The execution behavior varies depending on the current circuit state:
   *
   * <h4>CLOSED State Behavior:</h4>
   *
   * <ul>
   *   <li>Executes the supplier function normally
   *   <li>Monitors execution for failures (exceptions or policy violations)
   *   <li>Increments failure counter on errors
   *   <li>Resets failure counter on success
   *   <li>Transitions to OPEN if failure threshold is exceeded
   * </ul>
   *
   * <h4>HALF_OPEN State Behavior:</h4>
   *
   * <ul>
   *   <li>Allows limited calls through to test service recovery
   *   <li>Increments success counter on successful calls
   *   <li>Transitions to CLOSED if success threshold is met
   *   <li>Transitions to OPEN immediately on any failure
   * </ul>
   *
   * <h4>OPEN State Behavior:</h4>
   *
   * <ul>
   *   <li>Rejects calls immediately without executing supplier
   *   <li>Executes fallback if configured, otherwise throws exception
   *   <li>Automatically transitions to HALF_OPEN when break duration expires
   * </ul>
   *
   * @param supplier the function to execute with circuit breaker protection. Must not be null and
   *     should represent the primary operation that may fail and needs protection.
   * @return the result from successful supplier execution, fallback execution, or success policy
   *     validation.
   */
  Map<String, Serializable> call(Supplier<Map<String, Serializable>> supplier);
}
