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
package com.amazon.aocagent.installers.otinstallers;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazon.aocagent.helpers.SSHHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.EC2Service;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class OTPackageInstaller implements OTInstaller {
  Context context;
  SSHHelper sshHelper;
  MustacheHelper mustacheHelper;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;

    prepareSSHKey(this.context);
    // init sshHelper
    this.sshHelper =
            new SSHHelper(
                    this.context.getTestingAMI().getLoginUser(),
                    this.context.getInstancePublicIpAddress(),
                    GenericConstants.SSH_CERT_LOCAL_PATH.getVal());

    // wait until the instance is ready to login
    log.info("wait until the instance is ready to login");
    RetryHelper.retry(() -> sshHelper.isSSHReady());
    // handle firewall
    if (context.getTestingAMI().getDisableFirewallCommand() != null) {
      sshHelper.executeCommands(Arrays.asList(context.getTestingAMI().getDisableFirewallCommand()));
    }

    this.mustacheHelper = new MustacheHelper();
  }

  @Override
  public void installAndStart() throws Exception {
    downloadPackage();
    installPackage();
    configureAndStart();
  }

  private void downloadPackage() throws Exception {
    // get downloading link
    String s3Key = context.getTestingAMI().getS3Package().getS3Key(context.getAgentVersion());
    String downloadingLink =
            "https://" + context.getStack().getS3BucketName() + ".s3.amazonaws.com/" + s3Key;

    // get downloading command
    String downloadingCommand =
            context
                    .getTestingAMI()
                    .getDownloadingCommand(
                            downloadingLink,
                            context.getTestingAMI().getS3Package().getPackageName());

    // execute downloading command
    RetryHelper.retry(
        () -> {
          sshHelper.executeCommands(Arrays.asList(downloadingCommand));
        });
  }

  private void installPackage() throws Exception {
    // get installing command
    String installingCommand =
            context
                    .getTestingAMI()
                    .getInstallingCommand(context.getTestingAMI().getS3Package().getPackageName());

    // execute installing command
    RetryHelper.retry(
        () -> {
          sshHelper.executeCommands(Arrays.asList(installingCommand));
        });
  }

  private void configureAndStart() throws Exception {
    // generate configuration file
    String configContent = mustacheHelper.render(context.getOtConfig(), context);

    // write config onto the remote instance
    String configuringCommand = context.getTestingAMI().getConfiguringCommand(configContent);

    RetryHelper.retry(
        () -> {
          sshHelper.executeCommands(Arrays.asList(configuringCommand));
        });
    // start ot collector
    String startingCommand = context.getTestingAMI()
            .getStartingCommand(GenericConstants.EC2_CONFIG_PATH.getVal());
    RetryHelper.retry(
        () -> {
          sshHelper.executeCommands(Arrays.asList(startingCommand));
        });
  }

  private void prepareSSHKey(final Context context) throws Exception {
    EC2Service ec2Service = new EC2Service(context.getStack().getTestingRegion());
    // download the ssh keypair from s3
    ec2Service.downloadSSHKey(
            context.getStack().getSshKeyS3BucketName(),
            GenericConstants.SSH_KEY_NAME.getVal(),
            GenericConstants.SSH_CERT_LOCAL_PATH.getVal());
  }
}
