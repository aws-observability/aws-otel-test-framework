package com.amazon.aoc.callers;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.SampleAppResponse;
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
  }

  @Override
  public SampleAppResponse callSampleApp() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    AtomicReference<String> responseContent = new AtomicReference<>();
    RetryHelper.retry(
        60,
        () -> {
          try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("response from sample app {}", responseBody);
            if (!response.isSuccessful()) {
              throw new BaseException(ExceptionCode.DATA_EMITTER_UNAVAILABLE);
            }
            responseContent.set(responseBody);
          }
        });

    return new ObjectMapper().readValue(responseContent.get(), SampleAppResponse.class);
  }
}
