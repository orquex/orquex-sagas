package co.orquex.sagas.task.groovy.test.fixture;

import co.orquex.sagas.domain.api.context.TransactionContext;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TestGroovyTransactionContext implements TransactionContext {

  private static final Map<String, Serializable> transactionContextMap = new HashMap<>();

  @Override
  public Serializable get(String key) {
    return transactionContextMap.get(key);
  }

  @Override
  public void put(String key, Serializable value) {
    transactionContextMap.put(key, value);
  }

  @Override
  public void remove(String key) {
    transactionContextMap.remove(key);
  }

  @Override
  public void clear() {
    transactionContextMap.clear();
  }
}
