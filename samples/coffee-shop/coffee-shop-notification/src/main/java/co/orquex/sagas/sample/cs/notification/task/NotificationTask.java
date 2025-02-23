package co.orquex.sagas.sample.cs.notification.task;

import co.orquex.sagas.domain.api.TaskImplementation;
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
    final var metaNotification = taskRequest.metadata().get("notification");

    if (metaNotification instanceof Boolean notification) {
      if (Boolean.TRUE.equals(notification)) {
        log.info("Notification sent successfully");
      } else {
        log.warn("Notification not sent");
      }
    }

    return taskRequest.payload();
  }

  @Override
  public String getKey() {
    return "notification-sender";
  }
}
