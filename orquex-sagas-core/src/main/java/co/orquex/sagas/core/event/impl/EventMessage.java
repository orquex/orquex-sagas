package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;

/**
 * Represents the message you want to trigger.
 *
 * @param <T> message type.
 */
@Getter
public final class EventMessage<T> implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  private T message;
  private Error error;

  public EventMessage() {}

  public EventMessage(T message, Error error) {
    this.message = message;
    this.error = error;
  }

  public EventMessage(T message) {
    this.message = message;
  }

  public EventMessage(Error error) {
    this.error = error;
  }

  public boolean hasError() {
    return error != null;
  }
}
