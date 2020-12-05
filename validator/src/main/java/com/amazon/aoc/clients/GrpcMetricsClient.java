package com.amazon.aoc.clients;

import io.grpc.Channel;
import lombok.extern.log4j.Log4j2;

import java.util.logging.Level;
import java.util.logging.Logger;

@Log4j2
public class GrpcMetricsClient implements GrpcClient {
  private final MetricsServiceGrpc.MetricsServiceBlockingStub blockingStub;

  public GrpcMetricsClient(Channel channel) {
    blockingStub = MetricsServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Check Data from GRPC metrics server.
   *
   */
  public String checkData(String name) {
    Metrichandler.Request request = Metrichandler.Request.newBuilder().setName(name).build();
    Metrichandler.CheckDataResponse response;
    try {
      response = blockingStub.checkData(request);
    } catch (Exception e) {
      log.warn("RPC failed: {}", e.getMessage());
      return null;
    }
    log.info("Check Metric Data: " + response.getMessage());
    return response.getMessage();
  }
}
