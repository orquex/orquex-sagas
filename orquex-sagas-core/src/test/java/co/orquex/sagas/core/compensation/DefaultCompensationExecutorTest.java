package co.orquex.sagas.core.compensation;

import static co.orquex.sagas.core.fixture.TaskProcessorFixture.getTaskProcessor;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.fixture.CompensationFixture;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.Task;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCompensationExecutorTest {

  public static final String TRANSACTION_ID = UUID.randomUUID().toString();

  @Mock Registry<TaskExecutor> taskExecutorRegistry;
  @Mock TaskRepository taskRepository;
  @Mock CompensationRepository compensationRepository;

  @Mock TaskExecutor taskExecutor;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Task task;

  @InjectMocks DefaultCompensationExecutor defaultCompensationExecutor;

  @Test
  void shouldExecuteCompensation() {
    when(compensationRepository.findByTransactionId(TRANSACTION_ID))
        .thenReturn(CompensationFixture.getCompensations(TRANSACTION_ID, 3));
    when(taskExecutorRegistry.get(anyString())).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById(anyString())).thenReturn(Optional.of(task));
    when(task.configuration().executor()).thenReturn("task-executor");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Collections.emptyMap());

    defaultCompensationExecutor.execute(TRANSACTION_ID);
    verify(taskExecutor, times(3))
        .execute(anyString(), any(Task.class), any(ExecutionRequest.class));
  }

  @Test
  void shouldContinueExceptionWhenTaskExecutorNotFound() {
    when(compensationRepository.findByTransactionId(TRANSACTION_ID))
        .thenReturn(CompensationFixture.getCompensations(TRANSACTION_ID, 3));
    when(taskRepository.findById(anyString())).thenReturn(Optional.of(task));
    when(task.configuration().executor()).thenReturn("task-executor");
    when(taskExecutorRegistry.get(anyString())).thenReturn(Optional.empty());

    assertThatCode(() -> defaultCompensationExecutor.execute(TRANSACTION_ID))
        .doesNotThrowAnyException();
    verify(taskExecutorRegistry, times(3)).get(anyString());
    verify(taskRepository, times(3)).findById(anyString());
  }

  @Test
  void shouldContinueWithTheExecutionWhenTaskNotFound() {
    when(compensationRepository.findByTransactionId(TRANSACTION_ID))
        .thenReturn(CompensationFixture.getCompensations(TRANSACTION_ID, 3));
    when(taskRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThatCode(() -> defaultCompensationExecutor.execute(TRANSACTION_ID))
        .doesNotThrowAnyException();
    verify(taskExecutorRegistry, never()).get(anyString());
    verify(taskRepository, times(3)).findById(anyString());
  }

  @Test
  void shouldExecuteCompensationEvenIfOtherFails() {
    when(compensationRepository.findByTransactionId(TRANSACTION_ID))
        .thenReturn(CompensationFixture.getCompensations(TRANSACTION_ID, 3));
    when(taskExecutorRegistry.get(anyString())).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById(anyString())).thenReturn(Optional.of(task));
    when(task.configuration().executor()).thenReturn("task-executor");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Collections.emptyMap())
        .thenThrow(WorkflowException.class)
        .thenReturn(Collections.emptyMap());

    defaultCompensationExecutor.execute(TRANSACTION_ID);
    verify(taskExecutor, times(3))
        .execute(anyString(), any(Task.class), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteProcessorsBeforeAndAfterCompensation() {
    final var compensations =
        List.of(
            CompensationFixture.getCompensation(
                TRANSACTION_ID,
                "task",
                getTaskProcessor("task-pre-processor"),
                getTaskProcessor("task-post-processor")));
    when(compensationRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(compensations);
    when(taskExecutorRegistry.get(anyString())).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById(anyString())).thenReturn(Optional.of(task));
    when(task.configuration().executor()).thenReturn("task-executor");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Collections.emptyMap());

    defaultCompensationExecutor.execute(TRANSACTION_ID);

    verify(taskExecutor, times(3))
        .execute(anyString(), any(Task.class), any(ExecutionRequest.class));
    verify(taskExecutorRegistry, times(3)).get(anyString());
    verify(taskRepository, times(3)).findById(anyString());
  }
}
