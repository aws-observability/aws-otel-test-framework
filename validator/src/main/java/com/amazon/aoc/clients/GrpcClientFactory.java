package com.amazon.aoc.clients;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClientFactory {

  /**
   * create and init GrpcClient base on testcase and target url.
   * @param testcase - testcase name
   * @param channel - the channel used to connect to grpc server
   * @return GrpcClient - either a GrpcMetricsClient or GrpcTraceClient
   */
  public GrpcClient getGrpcClient(String testcase, ManagedChannel channel) {
    switch (testcase) {
      case "otlp_grpc_exporter_metric_mock":
        return new GrpcMetricsClient(channel);
      case "otlp_grpc_exporter_trace_mock":
        return new GrpcTraceClient(channel);
      default:
        return null;
    }
  }
}
