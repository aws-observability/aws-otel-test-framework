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

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SSHHelper {

  private String loginUser;
  private String host;
  private String certificatePath;

  /**
   * SSHHelper Constructor.
   *
   * @param loginUser the user used to login the host
   * @param host the host ip/hostname
   * @param certificatePath the ssh key path on the local
   */
  public SSHHelper(String loginUser, String host, String certificatePath) {
    this.loginUser = loginUser;
    this.host = host;
    this.certificatePath = certificatePath;
  }

  public void isSSHReady() throws Exception {
    this.executeCommands(Arrays.asList("uptime"));
  }

  /**
   * executeCommands executes a list of commands on the remote host.
   *
   * @param commands the list of the commands
   * @throws Exception when commands execution fail
   */
  public String executeCommands(List<String> commands) throws Exception {
    Channel channel = null;
    Session session = null;

    try {
      log.info("run remote commands on {}@{}, the rsa key is {}", loginUser, host, certificatePath);
      JSch jsch = new JSch();

      jsch.addIdentity(certificatePath);
      session = jsch.getSession(loginUser, host);
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.connect();
      session.setTimeout(Integer.parseInt(GenericConstants.SSH_TIMEOUT.getVal()));

      channel = session.openChannel("exec");
      ChannelExec channelExec = (ChannelExec) channel;

      String compositeCommand = String.join("&&", commands);
      log.info("run remote commands: {}", compositeCommand);
      channelExec.setCommand(compositeCommand);
      channelExec.setPty(true);

      InputStream in = channel.getInputStream();
      InputStream err = ((ChannelExec) channel).getErrStream();
      channelExec.connect();
      log.info("connected");

      StringBuilder outputBuffer = new StringBuilder();
      StringBuilder errorBuffer = new StringBuilder();
      byte[] tmp = new byte[1024];
      while (true) {
        while (in.available() > 0) {
          int i = in.read(tmp, 0, 1024);
          if (i < 0) {
            break;
          }
          outputBuffer.append(new String(tmp, 0, i, StandardCharsets.UTF_8));
        }
        while (err.available() > 0) {
          int i = err.read(tmp, 0, 1024);
          if (i < 0) {
            break;
          }
          errorBuffer.append(new String(tmp, 0, i, StandardCharsets.UTF_8));
        }
        if (channel.isClosed()) {
          if ((in.available() > 0) || (err.available() > 0)) {
            continue;
          }
          log.info("exit-status: " + channel.getExitStatus());
          break;
        }
        log.info("pulling: wait for command finished");
        TimeUnit.MILLISECONDS.sleep(
            Integer.parseInt(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()));
      }

      log.info("remote command executing std output: {}", outputBuffer.toString());
      log.info("remote command executing err output: {}", errorBuffer.toString());

      if (channel.getExitStatus() != 0) {
        throw new BaseException(
            ExceptionCode.SSH_COMMAND_FAILED, "execute remote command failed " + compositeCommand);
      }

      return outputBuffer.toString();

    } finally {
      if (channel != null) {
        channel.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
  }
}
