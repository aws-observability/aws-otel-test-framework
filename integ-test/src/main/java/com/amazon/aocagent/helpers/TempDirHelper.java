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
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class TempDirHelper {
  private static String delimiter = "-";
  private static Path topLevelPath = Paths.get(System.getProperty("java.io.tmpdir"), "AOCTestTemp");

  /**
   * delete those temp dirs which was created 2 hours ago.
   *
   * @throws IOException fail to delete dirs
   */
  public static void cleanTempDirs() throws IOException {
    File[] subDirs = topLevelPath.toFile().listFiles();
    if (subDirs != null) {
      for (File subdir : subDirs) {
        String name = subdir.getName();
        String[] elements = name.split(delimiter);
        if (elements.length == 2) {
          String timestamp = elements[1];
          // delete dir if it was created two hours ago
          if (new Date(Long.parseLong(timestamp))
              .before(
                  new DateTime()
                      .minusMinutes(
                          Integer.parseInt(GenericConstants.RESOURCE_CLEAN_THRESHOLD.getVal()))
                      .toDate())) {
            FileUtils.deleteDirectory(subdir);
          }
        } else {
          // delete the dir with unexpected subdir name
          FileUtils.deleteDirectory(subdir);
        }
      }
    }
  }

  private Path path;
  private String dirPrefix;

  /**
   * constructor of TempDirHelper.
   *
   * @param dirPrefix prefix of the temp dir which is going to be created
   */
  public TempDirHelper(String dirPrefix) {
    this.dirPrefix = dirPrefix;
  }

  /**
   * get the temp dir created by this object.
   *
   * @return path
   */
  public Path getPath() {
    if (path == null) {
      path =
          Paths.get(
              topLevelPath.toString(),
              String.format("%s%s%d", dirPrefix, delimiter, System.currentTimeMillis()));
      path.toFile().mkdir();
    }
    return path;
  }

  /**
   * delete the temp dir created by this object.
   *
   * @throws IOException fail to delete file
   */
  public void deletePath() throws IOException {
    FileUtils.deleteDirectory(path.toFile());
    path = null;
  }
}
