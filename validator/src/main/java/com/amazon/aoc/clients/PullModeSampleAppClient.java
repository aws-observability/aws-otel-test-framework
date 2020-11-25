package com.amazon.aoc.clients;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.models.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PullModeSampleAppClient<T> {

  private final String endpoint;

  /**
   * construct PullModeSampleAppClient.
   */
  public PullModeSampleAppClient(Context context, String expectedResultPath) {
    this.endpoint = context.getEndpoint() + expectedResultPath;
  }

  /**
   * list expected metrics from the sample app.
   */
  public T getExpectedResult() throws IOException, BaseException {
    Request request = new Request.Builder().url(endpoint).build();
    return execute(request);
  }

  private T execute(Request request) throws IOException, BaseException {
    OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    Response response = client.newCall(request).execute();

    if (response.code() >= 300) {
      throw new BaseException(ExceptionCode.PULL_MODE_SAMPLE_APP_CLIENT_REQUEST_FAILED,
              response.body().string());
    }

    String body = response.body().string();
    return new ObjectMapper().readValue(body, new TypeReference<T>() {});
  }
}
