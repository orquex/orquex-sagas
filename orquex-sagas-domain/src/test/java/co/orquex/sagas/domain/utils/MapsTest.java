package co.orquex.sagas.domain.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapsTest {

  @Test
  void shouldMergeTwoMaps() {
    // Given
    final var left = Map.of("key1", "value1");
    final var right = Map.of("key2", "value2", "key1", "value3");

    // When
    final var result = Maps.merge(left, right);

    // Then
    assertEquals(2, result.size());
    assertEquals("value3", result.get("key1"));
    assertEquals("value2", result.get("key2"));
  }

  @Test
  void shouldMergeThreeMaps() {
    // Given
    final var left = Map.of("key1", "value1");
    final var right = Map.of("key2", "value2", "key1", "value3");
    final var other = Map.of("key3", "value3");

    // When
    final var result = Maps.merge(left, right, other);

    // Then
    assertEquals(3, result.size());
    assertEquals("value3", result.get("key1"));
    assertEquals("value2", result.get("key2"));
    assertEquals("value3", result.get("key3"));
  }
}
