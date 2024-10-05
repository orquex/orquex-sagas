package co.orquex.sagas.sample.cs.notification.task;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationTask implements TaskImplementation {

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    final var hasNotification = taskRequest.metadata().containsKey("notification");

    if (hasNotification) {
      log.info("Notification sent successfully");
      return taskRequest.payload();
    }

    throw new WorkflowException("Notification checkout failed");
  }

  @Override
  public String getKey() {
    return "notification-sender";
  }
}
