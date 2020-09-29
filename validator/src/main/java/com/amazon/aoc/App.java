package com.amazon.aoc;

import com.amazon.aoc.helpers.ContextBuildHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.validators.BatchedValidator;
import com.amazon.aoc.validators.MetricValidator;
import com.amazon.aoc.validators.TraceValidator;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class App {
  public static void main(String[] args) throws Exception {
    Context context = new ContextBuildHelper().buildContextFromEnvVars();
    log.info(context);
    BatchedValidator batchedValidator = new BatchedValidator(Arrays.asList(
      new MetricValidator(),
      new TraceValidator()
    ));

    batchedValidator.init(context);
    batchedValidator.validate();
  }
}
