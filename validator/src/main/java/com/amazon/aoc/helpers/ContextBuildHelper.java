package com.amazon.aoc.helpers;

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.fileconfigs.ExpectedMetric;
import com.amazon.aoc.fileconfigs.ExpectedTrace;
import com.amazon.aoc.models.Context;

public class ContextBuildHelper {
  public Context buildContextFromEnvVars(){
    return new Context(
      System.getenv(GenericConstants.ENV_VAR_AGENT_VERSION.getVal()),
      System.getenv(GenericConstants.ENV_VAR_INSTANCE_ID.getVal()),
      ExpectedMetric.valueOf(System.getenv(GenericConstants.ENV_VAR_EXPECTED_METRIC.getVal())),
      ExpectedTrace.valueOf(System.getenv(GenericConstants.ENV_VAR_EXPECTED_TRACE.getVal())),
      System.getenv(GenericConstants.ENV_VAR_NAMESPACE.getVal()),
      System.getenv(GenericConstants.ENV_VAR_DATA_EMITTER_ENDPOINT.getVal()),
      System.getenv(GenericConstants.ENV_VAR_REGION.getVal())
    );
  }
}
