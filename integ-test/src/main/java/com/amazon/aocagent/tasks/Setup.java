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
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.EC2Service;
import com.amazon.aocagent.services.IAMService;
import com.amazon.aocagent.services.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * This is the first task needed to be ran for any new developer, it creates the required aws
 * resources in your testing aws account, we assume you have already configure your aws account
 * credentials at ~/.aws/credentials, for more info about how to configure credentials please check
 * https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html.
 */
@Log4j2
public class Setup implements ITask {
  EC2Service ec2Service;
  S3Service s3Service;
  Context context;

  @Override
  public void init(Context context) throws Exception {
    ec2Service = new EC2Service(context.getStack().getTestingRegion());
    s3Service = new S3Service(context.getStack().getTestingRegion());
    this.context = context;
  }

  @Override
  public void execute() throws Exception {
    setupS3RelatedResources();
    setupEC2RelatedResources();
    dumpStack();
  }

  private void setupEC2RelatedResources() throws IOException, BaseException {
    log.info("create iam role to give permissions to emit data to CloudWatch");
    IAMService iamService = new IAMService(context.getStack().getTestingRegion());
    iamService.createIAMRoleIfNotExisted(GenericConstants.IAM_ROLE_NAME.getVal());

    log.info("create sshkey for ec2 instance login");
    ec2Service.createSSHKeyIfNotExisted(
        GenericConstants.SSH_KEY_NAME.getVal(), context.getStack().getSshKeyS3BucketName());

    log.info("create security group so that the instance could send requests out");
    ec2Service.createSecurityGroup(GenericConstants.SECURITY_GROUP_NAME.getVal());
  }

  private void setupS3RelatedResources() throws BaseException {
    // setup all the required s3 buckets
    for (String bucketName :
        Arrays.asList(
            context.getStack().getS3BucketName(),
            context.getStack().getS3ReleaseCandidateBucketName(),
            context.getStack().getSshKeyS3BucketName(),
            context.getStack().getTraceDataS3BucketName())) {
      try {
        s3Service.createBucket(bucketName);
      } catch (BaseException ex) {
        // don't quit if the bucket is in the current account
        if (ExceptionCode.S3_BUCKET_IS_EXISTED_IN_CURRENT_ACCOUNT.getCode() == ex.getCode()) {
          log.info("the bucket {} is already in current account", bucketName);
        } else {
          throw ex;
        }
      }
    }
  }

  private void dumpStack() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.writeValue(new File(this.context.getStackFilePath()), this.context.getStack());
  }
}
