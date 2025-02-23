package co.orquex.sagas.domain.api.registry;

/**
 * The Registrable interface provides a method to get a unique identifier of a registrable
 * implementation.
 */
public interface Registrable {

  /**
   * Returns an unique identifier of a registrable implementation.
   *
   * @return a registrable identifier.
   */
  String getKey();
}
