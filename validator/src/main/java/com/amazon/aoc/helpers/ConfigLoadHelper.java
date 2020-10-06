package com.amazon.aoc.helpers;

import com.amazon.aoc.models.ValidationConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Log4j2
public class ConfigLoadHelper {
  /**
   * load validation config from file, the base path is the resource path.
   * @param filePath the relative path under /resources/validations/
   * @return a list of validationconfig object
   * @throws IOException when the filepath is not existed
   */
  public List<ValidationConfig> loadConfigFromFile(String filePath) throws IOException {
    // todo support filepath which is not in the resource folder

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<ValidationConfig> validationConfigList =
        mapper.readValue(
            IOUtils.toString(getResourcePath(filePath)),
            new TypeReference<List<ValidationConfig>>() {});
    return validationConfigList;
  }

  private URL getResourcePath(String filePath) {
    return getClass().getResource("/validations/" + filePath);
  }
}
