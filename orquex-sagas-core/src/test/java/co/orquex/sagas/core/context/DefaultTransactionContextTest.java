package co.orquex.sagas.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultTransactionContextTest {

  private DefaultTransactionContext defaultTransactionContext;

  @Test
  void shouldCrateDefaultTransactionContext() {
    final Map<String, Serializable> transactionContext = Map.of("key", "value");
    defaultTransactionContext = new DefaultTransactionContext(transactionContext);
    assertThat(defaultTransactionContext.get("key")).isEqualTo("value");
  }

  @Test
  void shouldPutValueToTransactionContext() {
    final Map<String, Serializable> transactionContext = new HashMap<>();
    defaultTransactionContext = new DefaultTransactionContext(transactionContext);
    defaultTransactionContext.put("key", "value");
    assertThat(defaultTransactionContext.get("key")).isEqualTo("value");
  }

  @Test
  void shouldRemoveValueFromTransactionContext() {
    final Map<String, Serializable> transactionContext = new HashMap<>();
    defaultTransactionContext = new DefaultTransactionContext(transactionContext);
    defaultTransactionContext.put("key", "value");
    defaultTransactionContext.remove("key");
    assertThat(defaultTransactionContext.get("key")).isNull();
  }

  @Test
  void shouldClearTransactionContext() {
    final Map<String, Serializable> transactionContext = new HashMap<>();
    defaultTransactionContext = new DefaultTransactionContext(transactionContext);
    defaultTransactionContext.put("key", "value");
    defaultTransactionContext.clear();
    assertThat(defaultTransactionContext.get("key")).isNull();
  }
}
