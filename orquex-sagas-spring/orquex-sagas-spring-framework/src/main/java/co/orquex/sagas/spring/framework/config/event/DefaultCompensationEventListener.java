package co.orquex.sagas.spring.framework.config.event;

import co.orquex.sagas.core.event.EventListener;
import co.orquex.sagas.core.event.impl.EventMessage;
import co.orquex.sagas.domain.api.repository.CompensationRepository;
import co.orquex.sagas.domain.transaction.Compensation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultCompensationEventListener implements EventListener<Compensation> {

  private final CompensationRepository compensationRepository;

  @Override
  public void onMessage(EventMessage<Compensation> event) {
    final var compensation = event.message();
    log.debug(
        "Compensation event received of flow '{}' with correlation '{}' and transaction '{}', task '{}'",
        compensation.flowId(),
        compensation.correlationId(),
        compensation.transactionId(),
        compensation.task());
    compensationRepository.save(compensation);
    log.debug(
        "Compensation event saved of flow '{}' with correlation '{}' and transaction '{}', task '{}'",
        compensation.flowId(),
        compensation.correlationId(),
        compensation.transactionId(),
        compensation.task());
  }
}
