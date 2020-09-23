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
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazon.aocagent.services.SSMService;
import com.amazon.aocagent.models.Context;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class SsmOTPackageInstaller implements OTInstaller {
  Context context;
  MustacheHelper mustacheHelper;
  SSMService ssmService;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;

    // init ssmService
    this.ssmService = new SSMService(context.getRegion());
    RetryHelper.retry(Integer.valueOf(GenericConstants.MAX_RETRIES.getVal()),
            Integer.valueOf(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()) * 3,
        () -> {
          if (!ssmService.isInstanceReadyForSsm(this.context.getInstanceId())) {
            log.error("Instance with ID " + this.context.getInstanceId()
                    + " not ready for SSM in time. Check EC2 console.");
            throw new BaseException(ExceptionCode.EC2INSTANCE_STATUS_BAD);
          }
        });

    ssmService.updateSsmAgentToLatest(context.getInstanceId());

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
                    .getSsmDownloadingCommand(
                            downloadingLink,
                            context.getTestingAMI().getS3Package().getPackageName());

    // execute downloading command
    ssmService.runShellScriptCommand(context.getInstanceId(), Arrays.asList(downloadingCommand),
            context.getTestingAMI().getSSMDocument());

  }

  private void installPackage() throws Exception {
    // get installing command
    String installingCommand =
            context
                .getTestingAMI()
                .getSsmInstallingCommand(context.getTestingAMI().getS3Package().getPackageName());

    // execute installing command
    ssmService.runShellScriptCommand(context.getInstanceId(), Arrays.asList(installingCommand),
            context.getTestingAMI().getSSMDocument());
  }

  private void configureAndStart() throws Exception {
    // generate configuration file
    String configContent = mustacheHelper.render(context.getOtConfig(), context);

    List<String> ssmCommands = new ArrayList<>();
    // write config onto the remote instance
    String configuringCommand = context.getTestingAMI().getSsmConfiguringCommand(configContent);
    ssmCommands.add(configuringCommand);

    // start ot collector
    String startingCommand = context.getTestingAMI()
            .getSsmStartingCommand();
    ssmCommands.add(startingCommand);
    //Disable firewall so that the emitter can send metrics to it
    String disableFirewallCommand = context.getTestingAMI().getDisableFirewallCommand();
    if (disableFirewallCommand != null) {
      ssmCommands.add(disableFirewallCommand);
    }
    ssmService.runShellScriptCommand(context.getInstanceId(),
            ssmCommands, context.getTestingAMI().getSSMDocument());
  }
}
