package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents the message you want to trigger.
 *
 * @param <T> message type.
 */
@Getter
@Builder
public final class EventMessage<T> implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  private final T message;
  private final Error error;

  public boolean hasError() {
    return error != null;
  }
}
