package com.amazon.aoc.helpers;

import com.amazon.aoc.models.xray.Entity;

import java.util.List;

public final class SortUtils {
  private static final int MAX_RESURSIVE_DEPTH = 10;

  /**
   * Given a list of entities, which are X-Ray segments or subsegments, recursively sort each of
   * their children subsegments by start time, then sort the given list itself by start time.
   *
   * @param entities - list of X-Ray entities to sort recursively. Modified in place.
   */
  public static void recursiveEntitySort(List<Entity> entities) {
    recursiveEntitySort(entities, 0);
  }

  private static void recursiveEntitySort(List<Entity> entities, int depth) {
    if (entities == null || entities.size() == 0 || depth >= MAX_RESURSIVE_DEPTH) {
      return;
    }
    int currDepth = depth + 1;

    for (Entity entity : entities) {
      if (entity.getSubsegments() != null && !entity.getSubsegments().isEmpty()) {
        recursiveEntitySort(entity.getSubsegments(), currDepth);
      }
    }

    entities.sort(
        (entity1, entity2) -> {
          if (entity1.getStartTime() == entity2.getStartTime()) {
            return 0;
          }

          return entity1.getStartTime() < entity2.getStartTime() ? -1 : 1;
        }
    );
  }
}
