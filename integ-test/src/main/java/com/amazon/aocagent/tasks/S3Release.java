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

import com.amazon.aocagent.enums.LocalPackage;
import com.amazon.aocagent.enums.S3Package;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.S3Service;
import com.amazonaws.regions.Regions;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * S3Release will upload the packages[rpm, deb, msi, pkg] into the configured S3 bucket. it will be
 * used for AOC distribution as well as the pre-flight step in Integ-test to simulate the releases
 * in the Testing Stack
 */
@Log4j2
@Setter
public class S3Release implements ITask {
  private Context context;
  private S3Service s3Service;
  private String s3Bucket;

  @Override
  public void init(final Context context) throws Exception {
    this.context = context;

    // the global bucket is in us-east-1
    s3Service = new S3Service(Regions.US_EAST_1.getName());
    // bucket name is globally unique, so we use different bucket name for different stacks
    s3Bucket = context.getStack().getS3BucketName();
  }

  @Override
  public void execute() throws Exception {
    log.info("context: {}", this.context);
    this.releasePackagesToS3();
    this.printOutDownloadingLinks();
  }

  private void printOutDownloadingLinks() throws Exception {
    // generate a package download link table with markdown as part of release notes
    Table.Builder tableBuilder =
        new Table.Builder()
            .withAlignment(Table.ALIGN_CENTER)
            .addRow(new BoldText("Arch"), new BoldText("Platform"), new BoldText("Package"));

    for (S3Package s3Package : S3Package.values()) {
      tableBuilder.addRow(
          s3Package.getLocalPackage().getArchitecture().name(),
          s3Package.getSupportedOSDistribution().name(),
          "https://"
              + this.s3Bucket
              + ".s3.amazonaws.com/"
              + s3Package.getS3Key(context.getAgentVersion()));
    }

    log.info(tableBuilder.build().serialize());
  }

  /**
   * releasePackagesToS3 upload all the packages to the S3 bucket.
   *
   * @throws BaseException when one of packages is not existed locally
   */
  public void releasePackagesToS3() throws BaseException {
    // validate if local packages are existed
    validateLocalPackage();

    // upload local packages to s3 with versioned key
    uploadToS3(context.getAgentVersion(), false);

    // upload local packages to s3 with the "latest" key, override the key if it's existed
    uploadToS3("latest", true);
  }

  private void validateLocalPackage() throws BaseException {
    for (LocalPackage builtPackage : LocalPackage.values()) {
      // assuming the local directory is os/arch/version/
      String filePath = builtPackage.getFilePath(context.getLocalPackagesDir());
      if (!Files.exists(Paths.get(filePath))) {
        throw new BaseException(
            ExceptionCode.LOCAL_PACKAGE_NOT_EXIST, "local package not exist: " + filePath);
      }
    }
  }

  private void uploadToS3(String packageVersion, boolean override) throws BaseException {
    for (S3Package s3Package : S3Package.values()) {
      s3Service.uploadS3Object(
          s3Package.getLocalPackage().getFilePath(context.getLocalPackagesDir()),
          s3Bucket,
          s3Package.getS3Key(packageVersion),
          override);
    }
  }
}
