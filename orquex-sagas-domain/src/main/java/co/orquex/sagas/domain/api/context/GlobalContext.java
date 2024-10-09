package co.orquex.sagas.domain.api.context;

/** Expose the methods to manage the transaction contexts. */
public interface GlobalContext {

  /** Create or get a new transaction context. */
  TransactionContext get(String transactionId);

  /** Remove the transaction context. */
  void remove(String transactionId);
}
