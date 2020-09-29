package com.amazon.aoc.helpers;

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.fileconfigs.ExpectedMetric;
import com.amazon.aoc.fileconfigs.ExpectedTrace;
import com.amazon.aoc.models.Context;

public class ContextBuildHelper {
  public Context buildContextFromEnvVars(){
    Context context = new Context();

    context.setRegion(System.getenv(GenericConstants.ENV_VAR_REGION.getVal()));
    context.setAgentVersion(System.getenv(GenericConstants.ENV_VAR_AGENT_VERSION.getVal()));
    context.setTraceDataS3BucketName(System.getenv(GenericConstants.ENV_VAR_TRACE_S3_BUCKET.getVal()));
    context.setInstanceId(System.getenv(GenericConstants.ENV_VAR_INSTANCE_ID.getVal()));
    context.setExpectedMetric(ExpectedMetric.valueOf(System.getenv(GenericConstants.ENV_VAR_EXPECTED_METRIC.getVal())));
    context.setExpectedTrace(ExpectedTrace.valueOf(System.getenv(GenericConstants.ENV_VAR_EXPECTED_TRACE.getVal())));
    context.setNamespace(System.getenv(GenericConstants.ENV_VAR_NAMESPACE.getVal()));

    return context;
  }
}
