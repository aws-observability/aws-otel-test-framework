package com.amazon.aoc.helpers;

import com.amazon.aoc.models.xray.Entity;

import java.util.List;

public final class SortUtils {

  /**
   * Given a list of entities, which are X-Ray segments or subsegments, recursively sort each of
   * their children subsegments by start time, then sort the given list itself by start time.
   *
   * @param entities - list of X-Ray entities to sort recursively. Modified in place.
   */
  public static void recursiveEntitySort(List<Entity> entities) {
    if (entities == null || entities.size() == 0) {
      return;
    }

    for (Entity entity : entities) {
      if (entity.getSubsegments() != null && entity.getSubsegments().size() > 1) {
        recursiveEntitySort(entity.getSubsegments());
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
