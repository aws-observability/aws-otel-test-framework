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
package com.amazon.aocagent.commands;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.Stack;
import com.amazon.aocagent.tasks.TaskFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@CommandLine.Command(
    name = "setup",
    mixinStandardHelpOptions = true,
    description = "use for the stack setup of the aocintegtest")
@Log4j2
public class Setup implements Runnable {
  @CommandLine.Option(
      names = {"-s", "--stack"},
      description = "stack file path, .aoc-stack.yml by default",
      defaultValue = ".aoc-stack.yml")
  private String stackFilePath;

  @SneakyThrows
  @Override
  public void run() {
    Stack stack = null;
    if (!Files.exists(Paths.get(this.stackFilePath))) {
      log.info("no stack file at {}, Build stack with timestamp", this.stackFilePath);
      stack = this.buildStackWithTimestamp();
    } else {
      log.info("found stack file, Build stack from file");
      stack = this.buildStackFromFile();
    }

    Context context = new Context();
    context.setStackFilePath(this.stackFilePath);
    context.setStack(stack);
    TaskFactory.executeTask("Setup", context);
  }

  private Stack buildStackWithTimestamp() {
    Stack stack = new Stack();
    String timestamp = Long.toHexString(System.currentTimeMillis());

    stack.setSshKeyS3BucketName(
        String.join("-", GenericConstants.DEFAULT_SSH_KEY_S3_BUCKET_NAME.getVal(), timestamp));

    stack.setS3ReleaseCandidateBucketName(
        String.join("-", GenericConstants.DEFAULT_S3_RELEASE_CANDIDATE_BUCKET.getVal(), timestamp));

    stack.setS3BucketName(String.join("-", GenericConstants.DEFAULT_S3_BUCKET.getVal(), timestamp));

    stack.setTraceDataS3BucketName(
        String.join("-", GenericConstants.DEFAULT_TRACE_S3_BUCKET_NAME.getVal(), timestamp));

    stack.setTestingImageRepoName(
        GenericConstants.DEFAULT_DOCKER_IMAGE_REPO_NAME.getVal()
    );

    stack.setTestingRegion(GenericConstants.DEFAULT_REGION.getVal());

    return stack;
  }

  private Stack buildStackFromFile() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(
        new String(Files.readAllBytes(Paths.get(this.stackFilePath))), Stack.class);
  }
}
