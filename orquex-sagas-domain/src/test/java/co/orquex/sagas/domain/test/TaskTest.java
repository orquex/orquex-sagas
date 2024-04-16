package co.orquex.sagas.domain.test;

import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;
import static co.orquex.sagas.domain.test.JacksonFixture.readValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import co.orquex.sagas.domain.task.Task;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TaskTest {

  @Test
  void shouldCreateSimpleTaskWithJackson() {
    final var task = readValue("task-simple.json", Task.class);
    assertThat(task).isNotNull();
    assertAll(
        () -> assertThat(task.id()).isEqualTo("task-impl-id"),
        () -> assertThat(task.name()).isEqualTo("Task implementation name"),
        () -> assertThat(task.compensation()).isNotNull(),
        () -> assertThat(task.metadata()).isNotEmpty().hasSize(1),
        () -> assertThat(task.implementation()).isNotBlank(),
        () -> assertThat(task.configuration()).isNotNull());
    final var compensation = task.compensation();
    assertAll(
        () -> assertThat(compensation.task()).isEqualTo("task-comp-id"),
        () -> assertThat(compensation.metadata()).isEmpty());
    final var configuration = task.configuration();
    assertAll(
        () -> assertThat(configuration.resilience().timeout()).isEqualTo(Duration.ofSeconds(5)),
        () -> assertThat(configuration.executor()).isEqualTo(DEFAULT_EXECUTOR));
  }
}
