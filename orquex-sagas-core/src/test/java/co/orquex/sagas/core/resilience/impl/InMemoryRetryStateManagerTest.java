package co.orquex.sagas.core.resilience.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for InMemoryRetryStateManager.
 *
 * <p>Tests cover basic functionality, edge cases, thread-safety, and concurrent access scenarios.
 */
class InMemoryRetryStateManagerTest {

  InMemoryRetryStateManager stateManager;
  static final String RETRY_NAME = "test-retry";
  static final String ANOTHER_RETRY = "another-retry";

  @BeforeEach
  void setUp() {
    stateManager = new InMemoryRetryStateManager();
  }

  @Test
  @DisplayName("Should return 0 for value when retry state doesn't exist")
  void shouldReturnZeroValueForNonExistentRetryState() {
    // When & Then
    assertThat(stateManager.value(RETRY_NAME)).isZero();
  }

  @Test
  @DisplayName("Should return 1 for incrementAndGet when retry state doesn't exist")
  void shouldReturnOneForIncrementAndGetWhenRetryStateDoesNotExist() {
    // When & Then
    assertThat(stateManager.incrementAndGet(RETRY_NAME)).isOne();
  }

  @Test
  @DisplayName("Should add retry state and initialize to zero")
  void shouldAddRetryStateAndInitializeToZero() {
    // When
    stateManager.add(RETRY_NAME);

    // Then
    assertThat(stateManager.value(RETRY_NAME)).isZero();
  }

  @Test
  @DisplayName("Should increment retry count correctly after adding state")
  void shouldIncrementRetryCountCorrectlyAfterAddingState() {
    // Given
    stateManager.add(RETRY_NAME);

    // When
    final var firstIncrement = stateManager.incrementAndGet(RETRY_NAME);
    final var secondIncrement = stateManager.incrementAndGet(RETRY_NAME);
    final var thirdIncrement = stateManager.incrementAndGet(RETRY_NAME);

    // Then
    assertThat(firstIncrement).isEqualTo(1L);
    assertThat(secondIncrement).isEqualTo(2L);
    assertThat(thirdIncrement).isEqualTo(3L);
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(3L);
  }

  @Test
  @DisplayName("Should handle multiple retry states independently")
  void shouldHandleMultipleRetryStatesIndependently() {
    // Given
    stateManager.add(RETRY_NAME);
    stateManager.add(ANOTHER_RETRY);

    // When
    stateManager.incrementAndGet(RETRY_NAME);
    stateManager.incrementAndGet(RETRY_NAME);
    stateManager.incrementAndGet(ANOTHER_RETRY);

    // Then
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(2L);
    assertThat(stateManager.value(ANOTHER_RETRY)).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should overwrite existing retry state when adding same name")
  void shouldOverwriteExistingRetryStateWhenAddingSameName() {
    // Given
    stateManager.add(RETRY_NAME);
    stateManager.incrementAndGet(RETRY_NAME);
    stateManager.incrementAndGet(RETRY_NAME);
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(2L);

    // When - Add the same retry name again
    stateManager.add(RETRY_NAME);

    // Then - Should reset to 0
    assertThat(stateManager.value(RETRY_NAME)).isZero();
  }

  @Test
  @DisplayName("Should be thread-safe for concurrent add operations")
  void shouldBeThreadSafeForConcurrentAddOperations() throws InterruptedException {
    // Given
    final var threadCount = 10;
    final var latch = new CountDownLatch(threadCount);

    // When - Multiple threads try to add the same retry state
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                stateManager.add(RETRY_NAME);
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(10, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Should have exactly one entry with value 0
    assertThat(stateManager.value(RETRY_NAME)).isZero();
  }

  @Test
  @DisplayName("Should be thread-safe for concurrent increment operations")
  void shouldBeThreadSafeForConcurrentIncrementOperations() throws InterruptedException {
    // Given
    stateManager.add(RETRY_NAME);
    final var threadCount = 10;
    final var incrementsPerThread = 100;
    final var latch = new CountDownLatch(threadCount);
    final var totalIncrements = new AtomicLong(0);

    // When - Execute concurrent increment operations
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < incrementsPerThread; j++) {
                  stateManager.incrementAndGet(RETRY_NAME);
                  totalIncrements.incrementAndGet();
                }
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Final count should equal total increments
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(threadCount * incrementsPerThread);
    assertThat(totalIncrements.get()).isEqualTo(threadCount * incrementsPerThread);
  }

  @Test
  @DisplayName("Should be thread-safe for mixed operations")
  void shouldBeThreadSafeForMixedOperations() throws InterruptedException {
    // Given
    final var threadCount = 12;
    final var latch = new CountDownLatch(threadCount);

    // When - Execute mixed concurrent operations
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        final var threadIndex = i;
        executor.submit(
            () -> {
              try {
                if (threadIndex % 3 == 0) {
                  // Add operations
                  stateManager.add(RETRY_NAME + threadIndex);
                } else if (threadIndex % 3 == 1) {
                  // Increment operations
                  stateManager.add(RETRY_NAME);
                  for (int j = 0; j < 10; j++) {
                    stateManager.incrementAndGet(RETRY_NAME);
                  }
                } else {
                  // Value read operations
                  stateManager.add(RETRY_NAME);
                  for (int j = 0; j < 10; j++) {
                    stateManager.value(RETRY_NAME);
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Should not throw any exceptions and state should be consistent
    assertThat(stateManager.value(RETRY_NAME)).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("Should handle null retry names gracefully in value method")
  void shouldHandleNullRetryNamesInValueMethod() {
    // When & Then - Should not throw exception
    assertThat(stateManager.value(null)).isZero();
  }

  @Test
  @DisplayName("Should handle null retry names gracefully in incrementAndGet method")
  void shouldHandleNullRetryNamesInIncrementAndGetMethod() {
    // When & Then - Should not throw exception
    assertThat(stateManager.incrementAndGet(null)).isZero();
  }

  @Test
  @DisplayName("Should handle empty retry names")
  void shouldHandleEmptyRetryNames() {
    // Given
    final var emptyName = "";

    // When
    stateManager.add(emptyName);
    stateManager.incrementAndGet(emptyName);

    // Then
    assertThat(stateManager.value(emptyName)).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should handle whitespace-only retry names")
  void shouldHandleWhitespaceOnlyRetryNames() {
    // Given
    final var whitespaceName = "   ";

    // When
    stateManager.add(whitespaceName);
    stateManager.incrementAndGet(whitespaceName);
    stateManager.incrementAndGet(whitespaceName);

    // Then
    assertThat(stateManager.value(whitespaceName)).isEqualTo(2L);
  }

  @Test
  @DisplayName("Should handle very large retry counts")
  void shouldHandleVeryLargeRetryCounts() {
    // Given
    stateManager.add(RETRY_NAME);
    final var largeNumber = 1_000_000L;

    // When - Simulate large number of increments
    for (int i = 0; i < largeNumber; i++) {
      stateManager.incrementAndGet(RETRY_NAME);
    }

    // Then
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(largeNumber);
  }

  @Test
  @DisplayName("Should maintain atomicity during high-concurrency operations")
  void shouldMaintainAtomicityDuringHighConcurrencyOperations() throws InterruptedException {
    // Given
    stateManager.add(RETRY_NAME);
    final var threadCount = 20;
    final var operationsPerThread = 50;
    final var latch = new CountDownLatch(threadCount);

    // When - High concurrency increment operations
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  final var currentValue = stateManager.incrementAndGet(RETRY_NAME);
                  // Verify that each increment produces a positive result
                  assertThat(currentValue).isPositive();
                }
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(60, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Final count should be exactly the expected value
    assertThat(stateManager.value(RETRY_NAME)).isEqualTo(threadCount * operationsPerThread);
  }

  @Test
  @DisplayName("Should handle concurrent add and increment operations on same retry name")
  void shouldHandleConcurrentAddAndIncrementOperationsOnSameRetryName()
      throws InterruptedException {
    // Given
    final var threadCount = 10;
    final var latch = new CountDownLatch(threadCount);

    // When - Some threads add, others increment
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        final var threadIndex = i;
        executor.submit(
            () -> {
              try {
                if (threadIndex % 2 == 0) {
                  stateManager.add(RETRY_NAME);
                } else {
                  stateManager.incrementAndGet(RETRY_NAME);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      final var completed = latch.await(10, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // Then - Should have a consistent state (either 0 or some positive value)
    final var finalValue = stateManager.value(RETRY_NAME);
    assertThat(finalValue).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("Should maintain separate state spaces for different retry names")
  void shouldMaintainSeparateStateSpacesForDifferentRetryNames() {
    // Given
    final var retryNames = new String[] {"retry-1", "retry-2", "retry-3", "retry-4", "retry-5"};

    // When - Add different retry states and increment them differently
    for (int i = 0; i < retryNames.length; i++) {
      stateManager.add(retryNames[i]);
      for (int j = 0; j <= i; j++) {
        stateManager.incrementAndGet(retryNames[i]);
      }
    }

    // Then - Each retry should have its expected count
    for (int i = 0; i < retryNames.length; i++) {
      assertThat(stateManager.value(retryNames[i])).isEqualTo(i + 1);
    }
  }
}
