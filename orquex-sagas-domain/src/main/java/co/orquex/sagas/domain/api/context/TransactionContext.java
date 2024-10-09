package co.orquex.sagas.domain.api.context;

import java.io.Serializable;

/** Expose the methods to manage the data of a transaction context. */
public interface TransactionContext {

  Serializable get(String key);

  void put(String key, Serializable value);

  void remove(String key);

  void clear();
}
