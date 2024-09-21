package co.orquex.sagas.sample.cs.notification.task;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
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
  public Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {
    final var hasNotification = metadata.containsKey("notification");
    if (hasNotification) {
      log.info("Notification sent successfully");
      return payload;
    }
    throw new WorkflowException("Notification checkout failed");
  }

  @Override
  public String getKey() {
    return "notification-sender";
  }
}
