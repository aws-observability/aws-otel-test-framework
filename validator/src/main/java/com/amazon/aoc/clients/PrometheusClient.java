package com.amazon.aoc.clients;

import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.prometheus.PrometheusMetric;
import com.amazon.aoc.models.prometheus.PrometheusQueryResult;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

@Log4j2
public class PrometheusClient {
  private static final String APS_SERVICE_NAME = "aps";

  private final String cortexInstanceEndpoint;
  private final HttpClient httpClient;

  /**
   * construct PrometheusClient.
   */
  public PrometheusClient(Context context) {
    AWS4Signer signer = new AWS4Signer();
    signer.setServiceName(APS_SERVICE_NAME);
    signer.setRegionName(context.getRegion());

    this.httpClient = HttpClients.custom()
            .addInterceptorLast(
                    new AWSRequestSigningApacheInterceptor(APS_SERVICE_NAME, signer,
                            new ProfileCredentialsProvider()))
            .setRetryHandler(new DefaultHttpRequestRetryHandler())
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectionRequestTimeout(2000)
                            .build())
            .build();

    this.cortexInstanceEndpoint = context.getCortexInstanceEndpoint();
  }

  /**
   * list metrics from the cortex instance via the query endpoint.
   *
   * @param query the Prometheus expression query string
   * @param timestamp the evaluation timestamp
   */
  public List<PrometheusMetric> listMetrics(String query, String timestamp)
          throws IOException, URISyntaxException {
    HttpUrl rawUrl = HttpUrl.parse(cortexInstanceEndpoint);
    URI uri = new URIBuilder()
            .setScheme(Objects.requireNonNull(rawUrl).scheme())
            .setHost(rawUrl.host())
            .setPort(rawUrl.port())
            .setPath(rawUrl.encodedPath() + "/api/v1/query")
            .addParameter("query", query)
            .addParameter("time", timestamp)
            .build();

    HttpGet request = new HttpGet(uri);
    return execute(request);
  }

  private List<PrometheusMetric> execute(HttpGet request) throws IOException {
    HttpResponse response = httpClient.execute(request);

    if (response.getStatusLine().getStatusCode() >= 300) {
      throw new PrometheusClientException(IOUtils.toString(response.getEntity().getContent()));
    }

    String body = IOUtils.toString(response.getEntity().getContent());
    PrometheusQueryResult result =
            new ObjectMapper().readValue(body, PrometheusQueryResult.class);

    return result.getData().getResult();
  }

  static class PrometheusClientException extends RuntimeException {
    public PrometheusClientException(String message) {
      super(message);
    }
  }
}
