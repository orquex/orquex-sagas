package co.orquex.sagas.core.context;

import co.orquex.sagas.domain.api.context.TransactionContext;
import java.io.Serializable;
import java.util.Map;

/** Default in-memory implementation of {@link TransactionContext}. */
public class DefaultTransactionContext implements TransactionContext {

  private final Map<String, Serializable> transactionContext;

  public DefaultTransactionContext(Map<String, Serializable> transactionContext) {
    this.transactionContext = transactionContext;
  }

  @Override
  public Serializable get(String key) {
    return transactionContext.get(key);
  }

  @Override
  public void put(String key, Serializable value) {
    transactionContext.put(key, value);
  }

  @Override
  public void remove(String key) {
    transactionContext.remove(key);
  }

  @Override
  public void clear() {
    transactionContext.clear();
  }
}
