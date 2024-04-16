package co.orquex.sagas.domain.utils;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Maps {

  /**
   * Merge two maps creating a new Map instance, the left map will have less
   *
   * @param left map to be overwritten
   * @param right map that would overwrite
   * @return a new instance type with the value of the maps
   * @param <I> key data type
   * @param <O> value data type
   */
  public static <I, O> Map<I, O> merge(Map<I, O> left, Map<I, O> right) {
    final Map<I, O> newMap = new HashMap<>(left);
    newMap.putAll(right);
    return newMap;
  }
}
