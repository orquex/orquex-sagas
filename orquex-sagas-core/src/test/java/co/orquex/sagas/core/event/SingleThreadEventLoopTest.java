package co.orquex.sagas.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.core.event.impl.InMemoryEventSource;
import co.orquex.sagas.core.event.impl.SingleThreadEventLoop;
import co.orquex.sagas.core.fixture.EventListenerFixture;
import co.orquex.sagas.domain.event.Error;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SingleThreadEventLoopTest {

  @Test
  void testStart() {
    final var eventLoop = getSingleThreadEventLoop();
    assertThat(eventLoop.isAlive()).isTrue();
    eventLoop.stop();
  }

  @Test
  void testPushAndProcess() {
    final var listener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(listener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    final var message = new EventMessage<>("Test");
    eventLoop.push(message);
    await()
        .atMost(Duration.ofMillis(150))
        .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(message));
    eventLoop.stop();
  }

  @Test
  void testHasEvents() {
    final var eventLoop = getSingleThreadEventLoop();
    assertThat(eventLoop.hasEvents()).isFalse();
    eventLoop.stop();
  }

  @Test
  void testCustomQueueCapacityAndThreadName() {
    // Given
    final var customCapacity = 50;
    final var customThreadName = "custom-event-loop";

    // When
    final var listener = new EventListenerFixture<String>();
    final var eventSource = new InMemoryEventSource<String>();
    eventSource.addListener(listener);
    final var customEventLoop =
        SingleThreadEventLoop.of(eventSource, customCapacity, customThreadName);
    customEventLoop.start();

    try {
      // Then
      assertThat(customEventLoop.isAlive()).isTrue();

      // Push a message to verify it works
      final var message = new EventMessage<>("Custom test");
      customEventLoop.push(message);
      await()
          .atMost(Duration.ofMillis(200))
          .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(message));
    } finally {
      customEventLoop.stop();
    }
  }

  @Test
  void testBoundedQueueRejection() {
    // Given - create event loop with very small capacity
    final var smallCapacity = 2;
    final var smallQueueEventSource = new InMemoryEventSource<String>();
    final var slowListener = getSlowListener();
    smallQueueEventSource.addListener(slowListener);
    final var smallQueueEventLoop =
        SingleThreadEventLoop.of(smallQueueEventSource, smallCapacity, "small-queue-loop");
    smallQueueEventLoop.start();

    try {
      // When - try to push more messages than capacity
      for (int i = 0; i < 10; i++) {
        smallQueueEventLoop.push(new EventMessage<>("Message " + i));
      }

      // Then - some messages should be processed, but queue should have rejected some
      await()
          .atMost(Duration.ofSeconds(2))
          .untilAsserted(() -> assertThat(slowListener.getSuccessMessages()).hasSizeGreaterThan(0));
      // Not all messages will be processed due to queue capacity
      assertThat(slowListener.getSuccessMessages()).hasSizeLessThan(10);
    } finally {
      smallQueueEventLoop.stop();
    }
  }

  @Test
  void testGracefulShutdownProcessesRemainingEvents() {
    // Given - event loop with some queued messages
    final var eventListener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(eventListener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    // Give - push multiple messages
    final var messageCount = 10;
    for (int i = 0; i < messageCount; i++) {
      eventLoop.push(new EventMessage<>("Message " + i));
    }

    // When - stop the event loop
    await().atMost(Duration.ofMillis(150)).until(() -> !eventLoop.hasEvents());
    eventLoop.stop();

    // Then - all messages should be processed before shutdown
    assertThat(eventLoop.isAlive()).isFalse();
    await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(eventListener.getSuccessMessages()).hasSize(messageCount));
  }

  @Test
  void testStopIsIdempotent() {
    // Given - fresh event loop
    final var eventLoop = getSingleThreadEventLoop();
    // When - stop multiple times
    final var firstStop = eventLoop.stop();
    final var secondStop = eventLoop.stop();
    final var thirdStop = eventLoop.stop();

    // Then - all calls should succeed and event loop should be stopped
    assertThat(firstStop).isTrue();
    assertThat(secondStop).isTrue();
    assertThat(thirdStop).isTrue();
    assertThat(eventLoop.isAlive()).isFalse();
  }

  @Test
  void testConcurrentStartCalls() throws InterruptedException {
    // Given - fresh event loop
    final var freshEventSource = new InMemoryEventSource<String>();
    final var freshEventLoop = SingleThreadEventLoop.of(freshEventSource);

    // When - start from multiple threads concurrently
    final var threadCount = 10;
    final var latch = new CountDownLatch(threadCount);
    final var threads = new ArrayList<Thread>();

    for (int i = 0; i < threadCount; i++) {
      final var thread =
          new Thread(
              () -> {
                freshEventLoop.start();
                latch.countDown();
              });
      threads.add(thread);
      thread.start();
    }

    // Then - wait for all threads and verify the event loop started only once
    final var latchAwaitResult = latch.await(1, TimeUnit.SECONDS);
    assertThat(latchAwaitResult).isTrue();
    assertThat(freshEventLoop.isAlive()).isTrue();

    for (Thread thread : threads) {
      thread.join(100);
    }

    freshEventLoop.stop();
  }

  @Test
  void testPushToStoppedEventLoop() {
    // Given - running event loop
    final var eventListener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(eventListener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    // Given - stopped event loop
    eventLoop.stop();
    eventListener.getSuccessMessages().clear();

    // When - try to push a message
    final var message = new EventMessage<>("Should be ignored");
    eventLoop.push(message);

    // Then - message should not be processed
    await()
        .atMost(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(eventListener.getSuccessMessages()).isEmpty());
  }

  @Test
  void testConcurrentPushOperations() throws InterruptedException {
    // Given
    final var eventListener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(eventListener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    final var threadsCount = 5;
    final var messagesPerThread = 20;
    final var latch = new CountDownLatch(threadsCount);
    final var threads = new ArrayList<Thread>();

    // When - push messages from multiple threads
    for (int i = 0; i < threadsCount; i++) {
      final int threadId = i;
      final var thread =
          new Thread(
              () -> {
                for (int j = 0; j < messagesPerThread; j++) {
                  eventLoop.push(new EventMessage<>("Thread-" + threadId + "-Msg-" + j));
                }
                latch.countDown();
              });
      threads.add(thread);
      thread.start();
    }

    final var latchAwaitResult = latch.await(2, TimeUnit.SECONDS);
    assertThat(latchAwaitResult).isTrue();

    // Then - all messages should be processed
    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(
            () ->
                assertThat(eventListener.getSuccessMessages())
                    .hasSize(threadsCount * messagesPerThread));

    for (Thread thread : threads) {
      thread.join(100);
    }
  }

  @Test
  void testStartIsIdempotent() {
    // Given - fresh event loop
    final var listener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(listener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    // When - start multiple times
    eventLoop.start();
    eventLoop.start();
    eventLoop.start();

    // Then - event loop should still be alive and working
    assertThat(eventLoop.isAlive()).isTrue();

    final var message = new EventMessage<>("Test after multiple starts");
    eventLoop.push(message);
    await()
        .atMost(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(listener.getSuccessMessages()).contains(message));
  }

  @Test
  void testEventLoopProcessesErrorMessages() {
    // Given - event loop with a listener that records error messages
    final var listener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(listener);
    final var eventLoop = getSingleThreadEventLoop(eventSource);
    // Given - message with error
    final var error =
        Error.builder()
            .status("ERROR")
            .message("Test error")
            .detail("Error details for testing")
            .build();
    final var errorMessage = new EventMessage<String>(error);

    // When
    eventLoop.push(errorMessage);

    // Then - an error message should be processed
    await()
        .atMost(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(listener.getErrorMessages()).contains(errorMessage));
  }

  private static EventListenerFixture<String> getSlowListener() {
    return new EventListenerFixture<>() {
      @Override
      public void onMessage(EventMessage<String> message) {
        try {
          // Simulate slow processing to fill the queue
          System.out.println("Started processing message: " + message);
          Thread.sleep(500L);
          System.out.println("Finished processing message: " + message);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        super.onMessage(message);
      }
    };
  }

  private static InMemoryEventSource<String> getEventSource(EventListener<String> listener) {
    final var eventSource = new InMemoryEventSource<String>();
    eventSource.addListener(listener);
    return eventSource;
  }

  private static SingleThreadEventLoop<String> getSingleThreadEventLoop() {
    final var listener = new EventListenerFixture<String>();
    final var eventSource = getEventSource(listener);
    return getSingleThreadEventLoop(eventSource);
  }

  private static SingleThreadEventLoop<String> getSingleThreadEventLoop(
      EventSource<String> eventSource) {
    final var eventLoop = SingleThreadEventLoop.of(eventSource);
    eventLoop.start();
    return eventLoop;
  }
}
