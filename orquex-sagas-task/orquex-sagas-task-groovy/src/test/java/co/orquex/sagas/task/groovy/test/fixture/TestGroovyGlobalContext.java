package co.orquex.sagas.task.groovy.test.fixture;

import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.HashMap;
import java.util.Map;

public class TestGroovyGlobalContext implements GlobalContext {

  private static final Map<String, TransactionContext> globalContextMap = new HashMap<>();

  @Override
  public TransactionContext get(String transactionId) {
    return globalContextMap.computeIfAbsent(transactionId, k -> new TestGroovyTransactionContext());
  }

  @Override
  public void remove(String transactionId) {
    globalContextMap.remove(transactionId);
  }
}
