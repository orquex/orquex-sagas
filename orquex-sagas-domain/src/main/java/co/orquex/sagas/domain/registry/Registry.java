package co.orquex.sagas.domain.registry;

import java.util.Optional;

public interface Registry<T> {

  void add(T type);

  Optional<T> get(String id);
}
