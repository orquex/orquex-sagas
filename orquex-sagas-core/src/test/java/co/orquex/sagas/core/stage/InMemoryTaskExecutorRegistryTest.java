package co.orquex.sagas.core.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.orquex.sagas.domain.api.TaskExecutor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InMemoryTaskExecutorRegistryTest {

  private InMemoryTaskExecutorRegistry registry;

  @BeforeEach
  void setUp() {
    // Initialize an empty repository before each test
    registry = InMemoryTaskExecutorRegistry.newInstance();
  }

  @Test
  void shouldAddStageToRepository() {
    // Arrange
    final var taskExecutor = mock(TaskExecutor.class);
    when(taskExecutor.getId()).thenReturn("activity-1");
    // Act
    registry.add(taskExecutor);
    // Assert
    assertThat(registry.get("activity-1")).contains(taskExecutor);
  }

  @Test
  void shouldNotAffectOtherStages() {
    // Arrange
    final var taskExecutor1 = mock(TaskExecutor.class);
    when(taskExecutor1.getId()).thenReturn("activity-1");
    final var taskExecutor2 = mock(TaskExecutor.class);
    when(taskExecutor2.getId()).thenReturn("activity-2");

    // Act
    registry.add(taskExecutor1);

    // Assert
    assertThat(registry.get("activity-2")).isEmpty();
    assertThat(registry.get("activity-1")).contains(taskExecutor1);

    // Act
    registry.add(taskExecutor2);

    // Assert
    assertThat(registry.get("activity-1")).contains(taskExecutor1);
    assertThat(registry.get("activity-2")).contains(taskExecutor2);
  }

  @Test
  void shouldReturnEmptyForNonexistentStage() {
    // Act & Assert
    assertThat(registry.get("nonexistent")).isEmpty();
  }

  @Test
  void shouldCreateRepositoryWithGivenListStages() {
    // Arrange
    final var taskExecutor1 = mock(TaskExecutor.class);
    when(taskExecutor1.getId()).thenReturn("activity-1");
    final var taskExecutor2 = mock(TaskExecutor.class);
    when(taskExecutor2.getId()).thenReturn("activity-2");

    final List<TaskExecutor> taskExecutors = new ArrayList<>();
    taskExecutors.add(taskExecutor1);
    taskExecutors.add(taskExecutor2);

    // Act
    final var repository = InMemoryTaskExecutorRegistry.of(taskExecutors);

    // Assert
    assertThat(repository.get("activity-1")).contains(taskExecutor1);
    assertThat(repository.get("activity-2")).contains(taskExecutor2);
  }
}
