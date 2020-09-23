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
package com.amazon.aocagent.installers.emiterinstallers;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.enums.TestAMI;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazon.aocagent.helpers.SSHHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.EC2InstanceParams;
import com.amazon.aocagent.services.EC2Service;
import com.amazon.aocagent.testamis.ITestAMI;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class OTMetricAndTraceEmitterInstaller implements OTEmitterInstaller {
  Context context;
  SSHHelper sshHelper;
  EC2Service ec2Service;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.ec2Service = new EC2Service(context.getStack().getTestingRegion());
    this.launchEmitterInstance();
  }

  @Override
  public void installAndStart() throws Exception {
    // use host mode to interact with imds
    String dockerCommand =
        String.format(
            "sudo docker run --network host "
                + "-e S3_REGION=%s "
                + "-e TRACE_DATA_BUCKET=%s -e TRACE_DATA_S3_KEY=%s "
                + "-e OTEL_RESOURCE_ATTRIBUTES=service.namespace=%s,service.name=%s "
                + "-e INSTANCE_ID=%s "
                + "-e OTEL_OTLP_ENDPOINT=%s:55680 "
                + "-d %s",
            context.getStack().getTestingRegion(),
            context.getStack().getTraceDataS3BucketName(),
            context.getInstanceId(), // use instanceid as the s3 key of trace data
            GenericConstants.SERVICE_NAMESPACE.getVal(),
            GenericConstants.SERVICE_NAME.getVal(),
            context.getInstanceId(),
            context.getInstancePrivateIpAddress(),
            GenericConstants.TRACE_EMITTER_DOCKER_IMAGE_URL.getVal());

    RetryHelper.retry(() -> sshHelper.executeCommands(Arrays.asList(dockerCommand)));

    // wait until the trace emitter is ready to curl
    String curlCommand = String.format("curl %s", GenericConstants.TRACE_EMITTER_ENDPOINT.getVal());
    RetryHelper.retry(() -> sshHelper.executeCommands(Arrays.asList(curlCommand)));
  }

  private void launchEmitterInstance() throws Exception {
    // launch a suse instance to install emitter, because some of the old platform doesn't support
    // docker
    // so we have to use a separate instance to run emitter and use the private address as the otlp
    // endpoint
    ITestAMI testAMI = TestAMI.SUSE_15.getTestAMIObj();

    // tag instance for management
    TagSpecification tagSpecification =
        new TagSpecification()
            .withResourceType(ResourceType.Instance)
            .withTags(
                new Tag(
                    GenericConstants.EC2_INSTANCE_TAG_KEY.getVal(),
                    GenericConstants.EC2_INSTANCE_TAG_VAL.getVal()));

    Instance instance =
        ec2Service.launchInstance(
            EC2InstanceParams.builder()
                .amiId(testAMI.getAMIId())
                .instanceType(testAMI.getInstanceType())
                .iamRoleName(GenericConstants.IAM_ROLE_NAME.getVal())
                .securityGrpName(GenericConstants.SECURITY_GROUP_NAME.getVal())
                .tagSpecification(tagSpecification)
                .arch(testAMI.getS3Package().getLocalPackage().getArchitecture())
                .sshKeyName(GenericConstants.SSH_KEY_NAME.getVal())
                .build());

    // wait until the instance is ready to login
    // init sshHelper
    this.sshHelper =
        new SSHHelper(
            testAMI.getLoginUser(),
            instance.getPublicIpAddress(),
            GenericConstants.SSH_CERT_LOCAL_PATH.getVal());

    log.info("wait until the emitter instance is ready to login");
    RetryHelper.retry(() -> sshHelper.isSSHReady());

    // start docker service
    RetryHelper.retry(() -> sshHelper.executeCommands(Arrays.asList("sudo service docker start")));
  }
}
