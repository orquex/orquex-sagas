package co.orquex.sagas.core.resilience.impl;

import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.core.resilience.CircuitBreakerState.State;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for InMemoryCircuitBreakerStateManager.
 *
 * <p>Tests cover basic functionality, edge cases, thread-safety, and concurrent access scenarios.
 */
class InMemoryCircuitBreakerStateManagerTest {

  InMemoryCircuitBreakerStateManager stateManager;
  static final String CIRCUIT_NAME = "test-circuit";
  static final String ANOTHER_CIRCUIT = "another-circuit";

  @BeforeEach
  void setUp() {
    stateManager = new InMemoryCircuitBreakerStateManager();
  }

  @Test
  @DisplayName("Should return CLOSED state for non-existent circuit breaker")
  void shouldReturnDefaultClosedStateForNonExistentCircuit() {
    // When & Then
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.CLOSED);
  }

  @Test
  @DisplayName("Should set and get circuit breaker state correctly")
  void shouldSetAndGetStateCorrectly() {
    // When
    stateManager.setState(CIRCUIT_NAME, State.OPEN);

    // Then
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.OPEN);
  }

  @Test
  @DisplayName("Should transition through all circuit breaker states")
  void shouldTransitionThroughAllStates() {
    // Test CLOSED -> OPEN
    stateManager.setState(CIRCUIT_NAME, State.OPEN);
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.OPEN);

    // Test OPEN -> HALF_OPEN
    stateManager.setState(CIRCUIT_NAME, State.HALF_OPEN);
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.HALF_OPEN);

    // Test HALF_OPEN -> CLOSED
    stateManager.setState(CIRCUIT_NAME, State.CLOSED);
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.CLOSED);
  }

  @Test
  @DisplayName("Should return 0 for failure count when circuit breaker doesn't exist")
  void shouldReturnZeroFailureCountForNonExistentCircuit() {
    // When & Then
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isZero();
  }

  @Test
  @DisplayName("Should return 0 for success count when circuit breaker doesn't exist")
  void shouldReturnZeroSuccessCountForNonExistentCircuit() {
    // When & Then
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isZero();
  }

  @Test
  @DisplayName("Should increment failure count correctly")
  void shouldIncrementFailureCountCorrectly() {
    // When
    final var firstIncrement = stateManager.incrementFailureCount(CIRCUIT_NAME);
    final var secondIncrement = stateManager.incrementFailureCount(CIRCUIT_NAME);
    final var thirdIncrement = stateManager.incrementFailureCount(CIRCUIT_NAME);

    // Then
    assertThat(firstIncrement).isEqualTo(1L);
    assertThat(secondIncrement).isEqualTo(2L);
    assertThat(thirdIncrement).isEqualTo(3L);
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isEqualTo(3L);
  }

  @Test
  @DisplayName("Should increment success count correctly")
  void shouldIncrementSuccessCountCorrectly() {
    // When
    final var firstIncrement = stateManager.incrementSuccessCount(CIRCUIT_NAME);
    final var secondIncrement = stateManager.incrementSuccessCount(CIRCUIT_NAME);

    // Then
    assertThat(firstIncrement).isEqualTo(1L);
    assertThat(secondIncrement).isEqualTo(2L);
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isEqualTo(2L);
  }

  @Test
  @DisplayName("Should reset failure count to zero")
  void shouldResetFailureCountToZero() {
    // Given
    stateManager.incrementFailureCount(CIRCUIT_NAME);
    stateManager.incrementFailureCount(CIRCUIT_NAME);
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isEqualTo(2L);

    // When
    stateManager.resetFailureCount(CIRCUIT_NAME);

    // Then
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isZero();
  }

  @Test
  @DisplayName("Should reset success count to zero")
  void shouldResetSuccessCountToZero() {
    // Given
    stateManager.incrementSuccessCount(CIRCUIT_NAME);
    stateManager.incrementSuccessCount(CIRCUIT_NAME);
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isEqualTo(2L);

    // When
    stateManager.resetSuccessCount(CIRCUIT_NAME);

    // Then
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isZero();
  }

  @Test
  @DisplayName("Should return null for opened timestamp when not set")
  void shouldReturnNullForOpenedTimestampWhenNotSet() {
    // When & Then
    assertThat(stateManager.getOpenedTimestamp(CIRCUIT_NAME)).isNull();
  }

  @Test
  @DisplayName("Should set and get opened timestamp correctly")
  void shouldSetAndGetOpenedTimestampCorrectly() {
    // Given
    final var now = Instant.now();

    // When
    stateManager.setOpenedTimestamp(CIRCUIT_NAME, now);

    // Then
    assertThat(stateManager.getOpenedTimestamp(CIRCUIT_NAME)).isEqualTo(now);
  }

  @Test
  @DisplayName("Should handle multiple circuit breakers independently")
  void shouldHandleMultipleCircuitBreakersIndependently() {
    // Given & When
    stateManager.setState(CIRCUIT_NAME, State.OPEN);
    stateManager.setState(ANOTHER_CIRCUIT, State.HALF_OPEN);

    stateManager.incrementFailureCount(CIRCUIT_NAME);
    stateManager.incrementFailureCount(CIRCUIT_NAME);
    stateManager.incrementSuccessCount(ANOTHER_CIRCUIT);

    final var timestamp1 = Instant.now().minusSeconds(10);
    final var timestamp2 = Instant.now().minusSeconds(5);
    stateManager.setOpenedTimestamp(CIRCUIT_NAME, timestamp1);
    stateManager.setOpenedTimestamp(ANOTHER_CIRCUIT, timestamp2);

    // Then
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.OPEN);
    assertThat(stateManager.getState(ANOTHER_CIRCUIT)).isEqualTo(State.HALF_OPEN);

    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isEqualTo(2L);
    assertThat(stateManager.getFailureCount(ANOTHER_CIRCUIT)).isZero();

    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isZero();
    assertThat(stateManager.getSuccessCount(ANOTHER_CIRCUIT)).isEqualTo(1L);

    assertThat(stateManager.getOpenedTimestamp(CIRCUIT_NAME)).isEqualTo(timestamp1);
    assertThat(stateManager.getOpenedTimestamp(ANOTHER_CIRCUIT)).isEqualTo(timestamp2);
  }

  @Test
  @DisplayName("Should be thread-safe for concurrent operations")
  void shouldBeThreadSafeForConcurrentOperations() throws InterruptedException {
    // Given
    final var threadCount = 10;
    final var operationsPerThread = 100;
    final var latch = new CountDownLatch(threadCount);
    final var totalFailureIncrements = new AtomicLong(0);
    final var totalSuccessIncrements = new AtomicLong(0);

    // When - Execute concurrent operations
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  // Concurrent failure count increments
                  stateManager.incrementFailureCount(CIRCUIT_NAME);
                  totalFailureIncrements.incrementAndGet();

                  // Concurrent success count increments
                  stateManager.incrementSuccessCount(CIRCUIT_NAME);
                  totalSuccessIncrements.incrementAndGet();

                  // Concurrent state changes
                  stateManager.setState(CIRCUIT_NAME, State.values()[j % 3]);

                  // Concurrent timestamp updates
                  stateManager.setOpenedTimestamp(CIRCUIT_NAME, Instant.now());
                }
              } finally {
                latch.countDown();
              }
            });
      }

      // Wait for all threads to complete
      final var completed = latch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Verify final counts are correct
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME))
        .isEqualTo(threadCount * operationsPerThread);
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME))
        .isEqualTo(threadCount * operationsPerThread);
    assertThat(totalFailureIncrements.get()).isEqualTo(threadCount * operationsPerThread);
    assertThat(totalSuccessIncrements.get()).isEqualTo(threadCount * operationsPerThread);
  }

  @Test
  @DisplayName("Should handle concurrent reset operations safely")
  void shouldHandleConcurrentResetOperationsSafely() throws InterruptedException {
    // Given
    final var threadCount = 5;
    final var latch = new CountDownLatch(threadCount);

    // Pre-populate some counts
    for (int i = 0; i < 50; i++) {
      stateManager.incrementFailureCount(CIRCUIT_NAME);
      stateManager.incrementSuccessCount(CIRCUIT_NAME);
    }

    // When - Execute concurrent reset operations
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        final var threadIndex = i;
        executor.submit(
            () -> {
              try {
                if (threadIndex % 2 == 0) {
                  stateManager.resetFailureCount(CIRCUIT_NAME);
                } else {
                  stateManager.resetSuccessCount(CIRCUIT_NAME);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(10, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Verify counts are reset to zero
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isZero();
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isZero();
  }

  @Test
  @DisplayName("Should handle null circuit names gracefully in getState")
  void shouldHandleNullCircuitNamesInGetState() {
    // When & Then - Should not throw exception
    assertThat(stateManager.getState(null)).isEqualTo(State.CLOSED);
  }

  @Test
  @DisplayName("Should handle empty circuit names")
  void shouldHandleEmptyCircuitNames() {
    // Given
    final var emptyName = "";

    // When
    stateManager.setState(emptyName, State.OPEN);
    stateManager.incrementFailureCount(emptyName);

    // Then
    assertThat(stateManager.getState(emptyName)).isEqualTo(State.OPEN);
    assertThat(stateManager.getFailureCount(emptyName)).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should maintain state consistency after multiple operations")
  void shouldMaintainStateConsistencyAfterMultipleOperations() {
    // Given - Perform a sequence of operations
    stateManager.setState(CIRCUIT_NAME, State.OPEN);

    for (int i = 0; i < 5; i++) {
      stateManager.incrementFailureCount(CIRCUIT_NAME);
    }

    stateManager.setState(CIRCUIT_NAME, State.HALF_OPEN);

    for (int i = 0; i < 3; i++) {
      stateManager.incrementSuccessCount(CIRCUIT_NAME);
    }

    final var openedTime = Instant.now();
    stateManager.setOpenedTimestamp(CIRCUIT_NAME, openedTime);

    stateManager.setState(CIRCUIT_NAME, State.CLOSED);
    stateManager.resetFailureCount(CIRCUIT_NAME);

    // Then - Verify final state
    assertThat(stateManager.getState(CIRCUIT_NAME)).isEqualTo(State.CLOSED);
    assertThat(stateManager.getFailureCount(CIRCUIT_NAME)).isZero();
    assertThat(stateManager.getSuccessCount(CIRCUIT_NAME)).isEqualTo(3L);
    assertThat(stateManager.getOpenedTimestamp(CIRCUIT_NAME)).isEqualTo(openedTime);
  }
}
