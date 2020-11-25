package com.amazon.aoc.clients;

import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.prometheus.PrometheusMetric;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

public class ExpectedResultClient {

  private String endpoint;

  /**
   * construct ExpectedResultClient.
   */
  public ExpectedResultClient(Context context, String expectedResultPath) {
    this.endpoint = context.getEndpoint() + expectedResultPath;
  }

  /**
   * list expected metrics from the sample app.
   */
  public List<PrometheusMetric> getExpectedResults() throws IOException {
    Request request = new Request.Builder().url(endpoint).build();
    return execute(request);
  }

  private List<PrometheusMetric> execute(Request request) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();

    if (response.code() >= 300) {
      throw new ExpectedResultClientException(response.body().string());
    }

    String body = response.body().string();

    return new ObjectMapper().readValue(body, new TypeReference<List<PrometheusMetric>>() {});
  }

  static class ExpectedResultClientException extends RuntimeException {
    public ExpectedResultClientException(String message) {
      super(message);
    }
  }
}
