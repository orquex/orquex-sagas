package co.orquex.sagas.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import co.orquex.sagas.domain.api.context.TransactionContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class DefaultGlobalContextTest {

  @Test
  void shouldGetTransactionContext() {
    final var globalContext = new DefaultGlobalContext(new ConcurrentHashMap<>());
    final var transactionId = UUID.randomUUID().toString();
    final var transactionContext = globalContext.get(transactionId);
    assertThat(globalContext.get(transactionId)).isNotNull().isEqualTo(transactionContext);
  }

  @Test
  void shouldRemoveTransactionContext() {
    final var globalContextMap = new ConcurrentHashMap<String, TransactionContext>();
    final var globalContext = new DefaultGlobalContext(globalContextMap);
    final var transactionId = UUID.randomUUID().toString();
    globalContext.get(transactionId);
    globalContext.remove(transactionId);
    assertThat(globalContextMap.get(transactionId)).isNull();
  }
}
