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
package com.amazon.aocagent.services;

import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.models.Context;
import com.amazonaws.services.eks.AmazonEKS;
import com.amazonaws.services.eks.AmazonEKSClient;
import com.amazonaws.services.eks.model.Cluster;
import com.amazonaws.services.eks.model.DescribeClusterRequest;
import com.amazonaws.services.eks.model.DescribeClusterResult;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EKSService {
  private AmazonEKS eksClient;

  public EKSService(final String region) {
    this.eksClient = AmazonEKSClient.builder().withRegion(region).build();
  }

  /**
   * Get EKS Cluster.
   *
   * @param context test context
   */
  public Cluster getCluster(Context context) throws BaseException {
    DescribeClusterRequest describeClusterRequest =
        new DescribeClusterRequest().withName(context.getEksClusterName());
    DescribeClusterResult describeClusterResult = eksClient.describeCluster(describeClusterRequest);
    if (null == describeClusterResult.getCluster()) {
      throw new BaseException(ExceptionCode.EKS_CLUSTER_UNAVAIL);
    }
    return describeClusterResult.getCluster();
  }
}
