package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.transaction.Compensation;
import co.orquex.sagas.domain.transaction.Status;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompensationFixture {

  public static final String FLOW_ID = UUID.randomUUID().toString();
  public static final String CORRELATION_ID = UUID.randomUUID().toString();
  public static final String TASK = "simple-task-%s";

  public static Compensation getCompensation(String transactionId, String task) {
    return getCompensation(transactionId, task, null, null);
  }

  public static Compensation getCompensation(
      String transactionId, String task, TaskProcessor preProcessor, TaskProcessor postProcessor) {
    return new Compensation(
        UUID.randomUUID().toString(),
        transactionId,
        FLOW_ID,
        CORRELATION_ID,
        task,
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        preProcessor,
        postProcessor,
        Status.CREATED,
        Instant.now(),
        Instant.now());
  }

  public static List<Compensation> getCompensations(String transactionId, int size) {
    final var compensations = new Compensation[size];
    for (var i = 0; i < size; i++) {
      compensations[i] = getCompensation(transactionId, TASK.formatted(i));
    }
    return List.of(compensations);
  }
}
