package com.amazon.aoc.validators;

import com.amazon.aoc.callers.HttpCaller;
import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;

public class ValidatorFactory {
  private Context context;

  public ValidatorFactory(Context context) {
    this.context = context;
  }

  /**
   * create and init validator base on config.
   * @param validationConfig config from file
   * @return validator object
   * @throws Exception when there's no matched validator
   */
  public IValidator launchValidator(ValidationConfig validationConfig) throws Exception {
    // get validator
    IValidator validator;
    FileConfig expectedData;
    switch (validationConfig.getValidationType()) {
      case "trace":
        validator = new TraceValidator();
        expectedData = validationConfig.getExpectedTraceTemplate();
        break;
      case "metric":
        validator = new MetricValidator();
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      default:
        throw new BaseException(ExceptionCode.VALIDATION_TYPE_NOT_EXISTED);
    }

    // get caller
    ICaller caller;
    switch (validationConfig.getCallingType()) {
      case "http":
        caller = new HttpCaller(context.getEndpoint(), validationConfig.getHttpPath());
        break;
      default:
        throw new BaseException(ExceptionCode.CALLER_TYPE_NOT_EXISTED);
    }

    // init validator
    validator.init(this.context, caller, expectedData);
    return validator;
  }
}
