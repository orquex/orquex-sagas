package co.orquex.sagas.core.event.impl;

import co.orquex.sagas.domain.event.Error;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the message that is intended to be triggered in the event system. This class is a
 * record, which is a special kind of class in Java that is intended to be a transparent holder for
 * immutable data.
 *
 * @param <T> the type of the message that this event carries. This allows for flexibility in the
 *     type of information that can be passed in an event.
 */
public record EventMessage<T>(T message, Error error) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  /**
   * Constructor that creates an EventMessage with a message and no error.
   *
   * @param message the message to be carried by the event
   */
  public EventMessage(T message) {
    this(message, null);
  }

  /**
   * Constructor that creates an EventMessage with an error and no message.
   *
   * @param error the error to be carried by the event
   */
  public EventMessage(Error error) {
    this(null, error);
  }

  /**
   * Checks if the EventMessage has an error.
   *
   * @return true if the EventMessage has an error, false otherwise
   */
  public boolean hasError() {
    return error != null;
  }
}
