package co.orquex.sagas.core.context;


import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Default in-memory implementation of {@link GlobalContext}. */
@Slf4j
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
    final var transactionContext = globalContextMap.remove(transactionId);
    log.debug("Transaction '{}' context removed: {}", transactionId, transactionContext != null);
  }
}
