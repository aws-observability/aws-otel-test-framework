/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.aocagent.helpers;

import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Log4j2
public class CommandExecutionHelper {
  /**
   * Amount of time to wait for processes to finish. Probably way more time than needed, but want to
   * pick a large value since the test will fail if the timeout is reached.
   */
  private static final long TIMEOUT_IN_SECONDS = 360;

  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  /**
   * Useful for sending the stdout/stderr of a child process to our logger. When started in its own
   * thread, the thread will terminate automatically when the inputStream is given EOF (such as when
   * a process connected to that stream is terminated).
   *
   * <p>Note that Java's Process object has some pretty confusing names, its 'getInputStream' and
   * 'getErrorStream' return java InputStream's for OUR process to consume from. But they are the
   * stdout/stderr of the child process represented by the Process object.
   */
  @RequiredArgsConstructor
  private static class StreamRedirecter implements Runnable {

    private final InputStream inputStream;
    private final Consumer<String> streamConsumer;

    public void run() {
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      reader.lines().forEach(streamConsumer);
    }
  }

  /**
   * runChildProcess executes the command in a child process.
   *
   * @param command the command to be executed
   * @return output of the command execution
   * @throws BaseException when the command fails to execute
   */
  public static String runChildProcess(String command) throws BaseException {
    return runChildProcessInternal(command, new String[] {});
  }

  /**
   * runChildProcessWithAWSCred executes the command in a child process with aws credential.
   *
   * @param command the command to be executed
   * @return output of the command execution
   * @throws BaseException when the command fails to execute
   */
  public static String runChildProcessWithAWSCred(String command) throws BaseException {
    // construct environment variable array
    List<String> envList = new ArrayList();
    AWSCredentials credentials = DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
    envList.add("AWS_ACCESS_KEY_ID=" + credentials.getAWSAccessKeyId());
    envList.add("AWS_SECRET_ACCESS_KEY=" + credentials.getAWSSecretKey());
    if (credentials instanceof AWSSessionCredentials) {
      AWSSessionCredentials sessionCredentials = (AWSSessionCredentials) credentials;
      envList.add("AWS_SESSION_TOKEN=" + sessionCredentials.getSessionToken());
    }

    return runChildProcessInternal(command, envList.toArray(new String[0]));
  }

  /**
   * runChildProcessWithEnvs executes the command in a child process with environment variables.
   *
   * @param command the command to be executed
   * @param envs environment variables
   * @return output of the command execution
   * @throws BaseException when the command fails to execute
   */
  public static String runChildProcessWithEnvs(String command, String[] envs) throws BaseException {
    return runChildProcessInternal(command, envs);
  }

  private static String runChildProcessInternal(String command, String[] envs)
      throws BaseException {
    log.info("execute command: {}", command);
    StringBuilder output = new StringBuilder();

    Process p;
    Future<?> stdoutFuture;
    Future<?> stderrFuture;

    try {
      p = Runtime.getRuntime().exec(command, envs);

      // p is set up by default to just have pipes for its stdout/stderr, so those will buffer until
      // we set up consumers
      StreamRedirecter stdoutRedirector =
          new StreamRedirecter(
              p.getInputStream(),
              s -> {
                output.append(s).append("\n");
              });
      stdoutFuture = THREAD_POOL.submit(stdoutRedirector);

      StreamRedirecter stderrRedirector = new StreamRedirecter(p.getErrorStream(), log::error);
      stderrFuture = THREAD_POOL.submit(stderrRedirector);

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    try {
      stdoutFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
      stderrFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
      if (0 != p.waitFor()) {
        throw new BaseException(ExceptionCode.COMMAND_FAILED_TO_EXECUTE);
      }
    } catch (InterruptedException | ExecutionException e) {
      p.destroyForcibly();
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      p.destroyForcibly();
      throw new RuntimeException("Timed out while waiting for command to complete.", e);
    }
    return output.toString();
  }
}
