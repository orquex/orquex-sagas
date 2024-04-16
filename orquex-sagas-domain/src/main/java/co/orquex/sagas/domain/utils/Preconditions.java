package co.orquex.sagas.domain.utils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/** This class is to check the arguments of the different domains entities and value objects. */
public final class Preconditions {

  private Preconditions() {}

  public static <T> T checkArgumentNotNull(T input, String message) {
    if (input == null) throw new IllegalArgumentException(message);
    return input;
  }

  public static <T, X extends Throwable> T checkArgumentNotNull(
      T input, Supplier<? extends X> exceptionSupplier) throws X {
    if (input == null) throw exceptionSupplier.get();
    return input;
  }

  public static String checkArgumentNotEmpty(String input, String message) {
    if (input == null || input.isEmpty() || input.isBlank())
      throw new IllegalArgumentException(message);
    return input;
  }

  public static <T extends Collection<?>> T checkArgumentNotEmpty(T input, String message) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException(message);
    return input;
  }

  public static <T extends Map<?, ?>> T checkArgumentNotEmpty(T input, String message) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException(message);
    return input;
  }

  public static <T> T checkArgumentNotNullOrElse(T input, T def) {
    if (input == null) return def;
    return input;
  }
}
