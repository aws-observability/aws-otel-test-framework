package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.clients.GrpcClient;
import com.amazon.aoc.clients.GrpcClientFactory;
import com.amazon.aoc.clients.GrpcMetricsClient;
import com.amazon.aoc.clients.GrpcTraceClient;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

@Log4j2
public class MockedServerValidator implements IValidator {
  String mockedServerValidatingUrl;
  ICaller caller;
  String testcase;
  private static final String HTTP_IDENTIFIER = "http";

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    mockedServerValidatingUrl = context.getMockedServerValidatingUrl();
    this.caller = caller;
    this.testcase = context.getTestcase();
  }

  @Override
  public void validate() throws Exception {
    // hit the endpoint to generate data if need be
    if (caller != null) {
      caller.callSampleApp();
    }

    // hit the mocked server to validate to if it receives data
    if (isHttpServer()) {
      callHttpMockedServer();
    } else {
      callGrpcMockedServer();
    }
  }

  private void callHttpMockedServer() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(this.mockedServerValidatingUrl).build();

    RetryHelper.retry(
        () -> {
          Response response = client.newCall(request).execute();
          if (!response.isSuccessful()) {
            throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_AVAILABLE);
          }

          String responseBody = response.body().string();
          if (!responseBody.equalsIgnoreCase("success")) {
            throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_RECEIVE_DATA);
          }

          log.info("http mocked server validation passed");
        });
  }

  private void callGrpcMockedServer() throws Exception {
    ManagedChannel channel = ManagedChannelBuilder.forTarget(this.mockedServerValidatingUrl)
            .usePlaintext()
            .build();
    GrpcClient client = new GrpcClientFactory().getGrpcClient(this.testcase, channel);

    try {
      RetryHelper.retry(
          () -> {
            String response = client.checkData("");
            if (null == response) {
              throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_AVAILABLE);
            }

            if (!response.equalsIgnoreCase("success")) {
              throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_RECEIVE_DATA);
            }

            log.info("grpc mocked server validation passed");
          });
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private boolean isHttpServer() {
    return this.mockedServerValidatingUrl.startsWith(HTTP_IDENTIFIER);
  }
}
