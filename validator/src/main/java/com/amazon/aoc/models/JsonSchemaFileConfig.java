package com.amazon.aoc.models;

import com.amazon.aoc.fileconfigs.FileConfig;

public class JsonSchemaFileConfig implements FileConfig {
  private String path;

  public JsonSchemaFileConfig(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }
}
