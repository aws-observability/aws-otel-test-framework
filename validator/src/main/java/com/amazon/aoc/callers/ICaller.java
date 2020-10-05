package com.amazon.aoc.callers;

import com.amazon.aoc.models.TraceFromEmitter;

public interface ICaller {
  TraceFromEmitter callSampleApp(String url) throws Exception;
}
