package co.orquex.sagas.domain.utils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for validating method arguments and constructor parameters.
 *
 * <p>This class provides static methods to check the validity of arguments passed to methods
 * and constructors throughout the domain entities and value objects. All methods throw
 * {@link IllegalArgumentException} when validation fails.
 *
 * <p>The class follows a consistent naming pattern and provides clear error messages
 * to help developers identify validation failures quickly.
 */
public final class Preconditions {

  private Preconditions() {}

  /**
   * Checks that the specified condition is true.
   *
   * @param condition the condition to check
   * @param message the exception message if condition is false
   * @throws IllegalArgumentException if condition is false
   */
  public static void checkArgument(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Checks that the specified object reference is not null.
   *
   * @param <T> the type of the reference
   * @param input the object reference to check for nullity
   * @param message the exception message if input is null
   * @return the non-null reference that was validated
   * @throws IllegalArgumentException if input is null
   */
  public static <T> T checkArgumentNotNull(T input, String message) {
    if (input == null) throw new IllegalArgumentException(message);
    return input;
  }

  /**
   * Checks that the specified object reference is not null using a custom exception supplier.
   *
   * @param <T> the type of the reference
   * @param <X> the type of exception to throw
   * @param input the object reference to check for nullity
   * @param exceptionSupplier supplier that provides the exception to throw if input is null
   * @return the non-null reference that was validated
   * @throws X if input is null
   */
  public static <T, X extends Throwable> T checkArgumentNotNull(
      T input, Supplier<? extends X> exceptionSupplier) throws X {
    if (input == null) throw exceptionSupplier.get();
    return input;
  }

  /**
   * Checks that the specified string is not null or empty.
   *
   * @param input the string to check
   * @param message the exception message if input is null or empty
   * @return the validated string
   * @throws IllegalArgumentException if input is null or empty
   */
  public static String checkArgumentNotEmpty(String input, String message) {
    if (input == null || input.isBlank())
      throw new IllegalArgumentException(message);
    return input;
  }

  /**
   * Checks that the specified collection is not null or empty.
   *
   * @param <T> the type of the collection
   * @param input the collection to check
   * @param message the exception message if input is null or empty
   * @return the validated collection
   * @throws IllegalArgumentException if input is null or empty
   */
  public static <T extends Collection<?>> T checkArgumentNotEmpty(T input, String message) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException(message);
    return input;
  }

  /**
   * Checks that the specified map is not null or empty.
   *
   * @param <T> the type of the map
   * @param input the map to check
   * @param message the exception message if input is null or empty
   * @return the validated map
   * @throws IllegalArgumentException if input is null or empty
   */
  public static <T extends Map<?, ?>> T checkArgumentNotEmpty(T input, String message) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException(message);
    return input;
  }

  /**
   * Returns the input if it is not null, otherwise returns the default value.
   *
   * <p>This method provides a null-safe way to handle potentially null arguments
   * by supplying a default value when the input is null.
   *
   * @param <T> the type of the values
   * @param input the value to check for nullity
   * @param def the default value to return if input is null
   * @return input if not null, otherwise def
   */
  public static <T> T checkArgumentNotNullOrElse(T input, T def) {
    if (input == null) return def;
    return input;
  }
}
