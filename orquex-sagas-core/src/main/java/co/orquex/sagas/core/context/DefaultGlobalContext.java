package co.orquex.sagas.core.context;

import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Default in-memory implementation of {@link GlobalContext}. */
public class DefaultGlobalContext implements GlobalContext {

  private final Map<String, TransactionContext> globalContextMap;

  public DefaultGlobalContext(Map<String, TransactionContext> globalContextMap) {
    this.globalContextMap = globalContextMap;
  }

  @Override
  @NonNull
  public TransactionContext get(@NonNull String transactionId) {
    return globalContextMap.computeIfAbsent(
        transactionId, id -> new DefaultTransactionContext(new ConcurrentHashMap<>()));
  }

  @Override
  public void remove(@NonNull String transactionId) {
    globalContextMap.remove(transactionId);
  }
}
