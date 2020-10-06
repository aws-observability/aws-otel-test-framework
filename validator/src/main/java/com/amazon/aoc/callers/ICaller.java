package com.amazon.aoc.callers;

import com.amazon.aoc.models.SampleAppResponse;

public interface ICaller {
  SampleAppResponse callSampleApp() throws Exception;
}
