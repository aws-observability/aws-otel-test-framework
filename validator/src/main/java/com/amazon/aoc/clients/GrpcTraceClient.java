package com.amazon.aoc.clients;

import io.grpc.Channel;
import lombok.extern.log4j.Log4j2;

import java.util.logging.Level;
import java.util.logging.Logger;

@Log4j2
public class GrpcTraceClient implements GrpcClient {
  private final TraceServiceGrpc.TraceServiceBlockingStub blockingStub;

  public GrpcTraceClient(Channel channel) {
    blockingStub = TraceServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Check Data from GRPC trace server.
   *
   */
  public String checkData(String name) {
    Tracehandler.Request request = Tracehandler.Request.newBuilder().setName(name).build();
    Tracehandler.CheckDataResponse response;
    try {
      response = blockingStub.checkData(request);
    } catch (Exception e) {
      log.warn("RPC failed: {}", e.getMessage());
      return null;
    }
    log.info("Check Trace Data: " + response.getMessage());
    return response.getMessage();
  }
}
