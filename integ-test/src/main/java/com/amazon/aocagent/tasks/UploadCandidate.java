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
import com.amazon.aocagent.helpers.CommandExecutionHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.S3Service;
import com.amazonaws.regions.Regions;

/**
 * upload the tested packages to s3 as release candidates, use git commit to distinguish the
 * packages.
 */
public class UploadCandidate implements ITask {
  S3Service s3Service;
  Context context;
  static String COMMAND_TO_PACK = "tar -czvf %s %s";

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    s3Service = new S3Service(Regions.US_EAST_1.getName());
  }

  @Override
  public void execute() throws Exception {
    // archive the candidate packages, build tarball from local-packages dir
    CommandExecutionHelper.runChildProcess(
        String.format(
            COMMAND_TO_PACK,
            GenericConstants.CANDIDATE_PACK_TO.getVal(),
            context.getLocalPackagesDir()));

    // upload the zip file to s3
    s3Service.uploadS3ObjectWithPrivateAccess(
        GenericConstants.CANDIDATE_PACK_TO.getVal(),
        context.getStack().getS3ReleaseCandidateBucketName(),
        context.getGithubSha() + ".tar.gz",
        false);
  }
}
