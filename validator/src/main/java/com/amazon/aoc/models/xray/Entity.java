package com.amazon.aoc.models.xray;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Barebones class representing a X-Ray Entity, used for JSON deserialization with Jackson.
 * It is not exactly an entity because it includes fields that are only allowed in Segments
 * (e.g. origin, user) but for the purposes of the validator that is acceptable because those
 * fields will be ignored when they're not present in subsegments.
 */
@Getter
@Setter
public class Entity {
  private String name;
  private String id;
  private String parentId;
  private double startTime;
  private String resourceArn;
  private String user;
  private String origin;
  private String traceId;

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private double endTime;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private boolean fault;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private boolean error;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private boolean throttle;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private boolean inProgress;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private boolean inferred;

  private String namespace;

  private List<Entity> subsegments;

  private Map<String, Object> http;
  private Map<String, Object> aws;
  private Map<String, Object> sql;

  private Map<String, Map<String, Object>> metadata;
  private Map<String, Object> annotations;
}
