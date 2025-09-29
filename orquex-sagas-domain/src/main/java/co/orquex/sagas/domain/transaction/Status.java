package co.orquex.sagas.domain.transaction;

/**
 * General status used during the execution of a flow.
 *
 * @see Transaction
 * @see Compensation
 * @see Checkpoint
 */
public enum Status {
  CREATED,
  IN_PROGRESS,
  CANCELED,
  COMPLETED,
  ERROR
}
