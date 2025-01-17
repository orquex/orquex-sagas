package co.orquex.sagas.domain.api;

/**
 * The CompensationExecutor interface defines the methods for executing compensations in a saga
 * workflow. Implementations of this interface are responsible for handling the compensation logic
 * for a given transaction.
 */
public interface CompensationExecutor extends AsyncExecutable<String> {

  /**
   * Executes the compensation logic for the given transaction.
   *
   * @param transactionId the transaction identifier
   */
  @Override
  void execute(String transactionId);
}
