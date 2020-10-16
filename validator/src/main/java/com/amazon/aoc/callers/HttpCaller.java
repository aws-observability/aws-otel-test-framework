package com.amazon.aoc.callers;

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.SampleAppResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class HttpCaller implements ICaller {
  private String url;

  public HttpCaller(String endpoint, String path) {
    this.url = endpoint + path;
    log.info("validator is testing {} path", this.url);
  }

  @Override
  public SampleAppResponse callSampleApp() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    AtomicReference<SampleAppResponse> sampleAppResponseAtomicReference = new AtomicReference<>();
    RetryHelper.retry(
        3,
        () -> {
          try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("response from sample app {}", responseBody);
            if (!response.isSuccessful()) {
              throw new BaseException(ExceptionCode.DATA_EMITTER_UNAVAILABLE);
            }
            SampleAppResponse sampleAppResponse = null;
            try {
              sampleAppResponse =
                  new ObjectMapper().readValue(responseBody, SampleAppResponse.class);
            } catch (JsonProcessingException ex) {
              // try to get the trace id from header
              // this is a specific logic for xray sdk, which injects trace id in header
              log.info("getting trace id from header");
              //  X-Amzn-Trace-Id: Root=1-5f84a611-f2f5df6827016222af9d8b60
              String traceId =
                  response.header(GenericConstants.HTTP_HEADER_TRACE_ID.getVal()).substring(5);
              sampleAppResponse = new SampleAppResponse();
              sampleAppResponse.setTraceId(traceId);
            }
            sampleAppResponseAtomicReference.set(sampleAppResponse);
          }
        });

    return sampleAppResponseAtomicReference.get();
  }
}
