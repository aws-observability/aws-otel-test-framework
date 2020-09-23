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
import com.amazon.aocagent.tasks.TaskFactory;
import com.amazonaws.util.StringUtils;
import lombok.SneakyThrows;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;

@CommandLine.Command(
    name = "candidate",
    mixinStandardHelpOptions = true,
    description = "use for uploading and downloading the candidate packages")
public class Candidate implements Runnable {
  @CommandLine.Mixin CommonOption commonOption = new CommonOption();

  @CommandLine.Option(
      names = {"-t", "--candidate-task"},
      description = "candidate task: DownloadCandidate, UploadCandidate",
      defaultValue = "UploadCandidate")
  private String candidateTask;

  @CommandLine.Option(
      names = {"-g", "--github-sha"},
      description = "github sha, for example: e3f0e4ef43cc72e09b145d8a9c7b7357420d300f",
      defaultValue = "")
  private String githubSha;

  @SneakyThrows
  @Override
  public void run() {
    Context context = commonOption.buildContext();

    if (StringUtils.isNullOrEmpty(this.githubSha)) {
      this.githubSha =
          new String(
                  Files.readAllBytes(
                      Paths.get(
                          context.getLocalPackagesDir()
                              + "/"
                              + GenericConstants.GITHUB_SHA_FILE_NAME.getVal())))
              .trim();
    }
    context.setGithubSha(this.githubSha);
    TaskFactory.executeTask(candidateTask, context);
  }
}
