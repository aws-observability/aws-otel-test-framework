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
package com.amazon.aocagent.tasks;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.helpers.CommandExecutionHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.S3Service;
import com.amazonaws.regions.Regions;

import java.nio.file.Files;
import java.nio.file.Paths;

public class DownloadCandidate implements ITask {
  S3Service s3Service;
  Context context;

  static String COMMAND_TO_UNPACK = "tar -zxvf %s -C %s";

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.s3Service = new S3Service(Regions.US_EAST_1.getName());
  }

  @Override
  public void execute() throws Exception {
    // download candidate package from s3
    s3Service.downloadS3Object(
        context.getStack().getS3ReleaseCandidateBucketName(),
        context.getGithubSha() + ".tar.gz",
        GenericConstants.CANDIDATE_DOWNLOAD_TO.getVal());

    // unpack the tarball into cwd since it already keeps the folder structure
    CommandExecutionHelper.runChildProcess(
        String.format(
            COMMAND_TO_UNPACK,
            GenericConstants.CANDIDATE_DOWNLOAD_TO.getVal(),
            GenericConstants.CANDIDATE_UNPACK_TO.getVal()));

    // validate version
    String versionFromFile =
        new String(Files.readAllBytes(Paths.get(context.getLocalPackagesDir() + "/VERSION")))
            .trim();
    if (!context.getAgentVersion().equals(versionFromFile)) {
      throw new BaseException(
          ExceptionCode.VERSION_NOT_MATCHED,
          "version is not matched " + versionFromFile + ":" + context.getAgentVersion());
    }

    // validate github sha
    if (!context
        .getGithubSha()
        .equals(
            new String(Files.readAllBytes(Paths.get(context.getLocalPackagesDir() + "/GITHUB_SHA")))
                .trim())) {
      throw new BaseException(ExceptionCode.GITHUB_SHA_NOT_MATCHED);
    }
  }
}
