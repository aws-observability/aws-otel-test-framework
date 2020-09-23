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
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Filter;

public class AwsNetworkService {

  private AmazonEC2 ec2Client;
  private String region;

  /**
   * retrieve network param for running ECS.
   * @param region client region
   */
  public AwsNetworkService(String region) {
    this.region = region;
    ec2Client = AmazonEC2ClientBuilder.standard().withRegion(region).build();
  }

  /**
   * describe the account default security group and get both group id and vpc id.
   * @throws Exception fail to describe sec group
   */
  public DescribeSecurityGroupsResult describeDefaultSecurityGroup() throws Exception {
    DescribeSecurityGroupsRequest request =
        new DescribeSecurityGroupsRequest().withGroupNames("default");
    DescribeSecurityGroupsResult result = ec2Client.describeSecurityGroups(request);
    if (result.getSecurityGroups().isEmpty()) {
      throw new BaseException(ExceptionCode.NO_DEFAULT_SECURITY_GROUP);
    }
    return result;
  }

  /**
   * describe the subnets by default VPC id.
   * @param vpcId VPC Id
   * @throws Exception fail to describe subnets
   */
  public DescribeSubnetsResult describeVpcSubnets(String vpcId) throws Exception {
    DescribeSubnetsRequest request =
        new DescribeSubnetsRequest()
            .withFilters(new Filter().withName("vpc-id").withValues(vpcId));
    DescribeSubnetsResult result = ec2Client.describeSubnets(request);
    if (result.getSubnets().isEmpty()) {
      throw new BaseException(ExceptionCode.NO_AVAILABLE_SUBNET);
    }
    return result;
  }
}
