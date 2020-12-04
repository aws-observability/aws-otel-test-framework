package com.amazon.aoc.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazon.aoc.models.xray.Entity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SortUtilsTest {

  @Test
  public void testSingleLevelListSort() {
    final List<Entity> generated = generateEntities(3);
    final List<Entity> entities = new ArrayList<>();
    entities.add(0, generated.get(1));
    entities.add(1, generated.get(2));
    entities.add(2, generated.get(0));

    // Verify list is unsorted
    assertThat(entities).containsSequence(generated.get(1), generated.get(2), generated.get(0));
    SortUtils.recursiveEntitySort(entities);

    assertThat(entities).hasSize(3);
    assertThat(entities).containsSequence(generated.get(0), generated.get(1), generated.get(2));
  }


  /**
   * Expected entity structure of this test after sorting.
   * <p>
   * ent0 ent1  ent2
   *  |
   * ent3 ent4 ent5
   *       |
   *   ent6 ent7
   * </p>
   */
  @Test
  public void testNestedEntitySort() {
    final List<Entity> generated = generateEntities(8);
    final List<Entity> topEntities = new ArrayList<>();
    final List<Entity> midEntities = new ArrayList<>();
    final List<Entity> bottomEntities = new ArrayList<>();

    topEntities.add(0, generated.get(1));
    topEntities.add(1, generated.get(2));
    topEntities.add(2, generated.get(0));
    midEntities.add(0, generated.get(5));
    midEntities.add(1, generated.get(4));
    midEntities.add(2, generated.get(3));
    bottomEntities.add(0, generated.get(7));
    bottomEntities.add(1, generated.get(6));

    generated.get(0).setSubsegments(midEntities);
    generated.get(4).setSubsegments(bottomEntities);

    SortUtils.recursiveEntitySort(topEntities);

    assertThat(topEntities).hasSize(3);
    assertThat(topEntities).containsSequence(generated.get(0), generated.get(1), generated.get(2));
    assertThat(topEntities.get(0).getSubsegments()).hasSize(3);
    assertThat(topEntities.get(0).getSubsegments()).isEqualTo(midEntities);
    assertThat(midEntities).containsSequence(generated.get(3), generated.get(4), generated.get(5));
    assertThat(midEntities.get(1).getSubsegments()).hasSize(2);
    assertThat(midEntities.get(1).getSubsegments()).isEqualTo(bottomEntities);
    assertThat(bottomEntities).containsSequence(generated.get(6), generated.get(7));
  }

  @Test
  public void testInfiniteLoop() {
    Entity current = new Entity();
    List<Entity> entityList = new ArrayList<>();
    entityList.add(current);
    current.setSubsegments(entityList);  // set up an infinite children loop

    SortUtils.recursiveEntitySort(entityList);

    // Not really testing anything, just making sure we don't infinite loop
    assertThat(entityList).hasSize(1);
  }

  private List<Entity> generateEntities(int n) {
    List<Entity> ret = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      Entity entity = new Entity();
      entity.setStartTime(i);
      ret.add(entity);
    }

    return ret;
  }
}
