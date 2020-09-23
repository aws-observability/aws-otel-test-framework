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

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazon.aocagent.models.EC2InstanceParams;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * EC2Service is a wrapper of Amazon EC2 Client.
 */
@Log4j2
public class EC2Service {
  private static final String ERROR_CODE_KEY_PAIR_NOT_FOUND = "InvalidKeyPair.NotFound";
  private static final String ERROR_CODE_KEY_PAIR_ALREADY_EXIST = "InvalidKeyPair.Duplicate";
  private static final String ERROR_CODE_SECURITY_GROUP_ALREADY_EXIST = "InvalidGroup.Duplicate";
  private static final String ERROR_CODE_SECURITY_GROUP_NOT_FOUND = "InvalidGroup.NotFound";
  private AmazonEC2 amazonEC2;
  private String region;
  private S3Service s3Service;
  private SSMService ssmService;

  /**
   * Construct ec2 service base on region.
   *
   * @param region the region to launch ec2 instance
   */
  public EC2Service(String region) throws Exception {
    this.region = region;
    amazonEC2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();
    s3Service = new S3Service(region);
    ssmService = new SSMService(region);
  }

  public SSMService getSsmService() {
    return ssmService;
  }

  /**
   * launchInstance launches one ec2 instance.
   *
   * @param params the instance setup configuration params
   * @return InstanceID
   */
  public Instance launchInstance(EC2InstanceParams params)
          throws Exception {
    // create request
    RunInstancesRequest runInstancesRequest =
        new RunInstancesRequest()
          .withImageId(params.getAmiId())
          .withMonitoring(false)
          .withMaxCount(1)
          .withMinCount(1)
          .withTagSpecifications(params.getTagSpecification())
          .withKeyName(params.getSshKeyName())
          .withSecurityGroupIds(getOrCreateSecurityGroupByName(params.getSecurityGrpName()))
          .withIamInstanceProfile(
                  new IamInstanceProfileSpecification().withName(params.getIamRoleName()))
          .withInstanceType(params.getInstanceType());
    if (!Strings.isNullOrEmpty(params.getUserData())) {
      runInstancesRequest.withUserData(params.getUserData());
    }

    RunInstancesResult runInstancesResult = amazonEC2.runInstances(runInstancesRequest);

    // return the first instance since only one instance gets launched
    Instance instance = runInstancesResult.getReservation().getInstances().get(0);

    // return the instance until it's ready
    return getInstanceUntilReady(instance.getInstanceId());
  }

  /**
   * listInstancesByTag gets ec2 instance info list based on the tag.
   *
   * @param tagName  tag key name
   * @param tagValue tag value
   * @return the list of ec2 instance
   */
  public List<Instance> listInstancesByTag(String tagName, String tagValue) {
    DescribeInstancesRequest describeInstancesRequest =
            new DescribeInstancesRequest()
                    .withFilters(new Filter("tag:" + tagName).withValues(tagValue));

    List<Instance> instanceList = new ArrayList<>();

    while (true) {
      DescribeInstancesResult describeInstancesResult =
              amazonEC2.describeInstances(describeInstancesRequest);
      for (Reservation reservation : describeInstancesResult.getReservations()) {
        instanceList.addAll(reservation.getInstances());
      }

      describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
      if (describeInstancesRequest.getNextToken() == null) {
        return instanceList;
      }
    }
  }

  /**
   * List all the ec2 instances.
   *
   * @return instance list
   */
  public List<Instance> listInstances() {
    List<Instance> instanceList = new ArrayList<>();
    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    while (true) {
      DescribeInstancesResult describeInstancesResult =
              amazonEC2.describeInstances(describeInstancesRequest);
      for (Reservation reservation : describeInstancesResult.getReservations()) {
        instanceList.addAll(reservation.getInstances());
      }

      describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
      if (describeInstancesRequest.getNextToken() == null) {
        return instanceList;
      }
    }
  }

  /**
   * terminateInstance terminates ec2 instances base on the instance id list.
   *
   * @param instanceIds ec2 instance ids to be terminated
   */
  public void terminateInstance(List<String> instanceIds) {
    log.info("clean instances {}", instanceIds);
    if (instanceIds.size() == 0) {
      return;
    }
    TerminateInstancesRequest terminateInstancesRequest =
            new TerminateInstancesRequest().withInstanceIds(instanceIds);

    amazonEC2.terminateInstances(terminateInstancesRequest);
  }

  private Instance getInstanceUntilReady(String targetInstanceId)
          throws Exception {
    DescribeInstancesRequest describeInstancesRequest =
            new DescribeInstancesRequest().withInstanceIds(targetInstanceId);

    AtomicReference<Instance> runningInstance = new AtomicReference<>();
    RetryHelper.retry(Integer.valueOf(GenericConstants.MAX_RETRIES.getVal()),
            Integer.valueOf(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()) * 3,
        () -> {
          DescribeInstancesResult describeInstancesResult =
                  amazonEC2.describeInstances(describeInstancesRequest);
          for (Reservation reservation : describeInstancesResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
              if (!targetInstanceId.equals(instance.getInstanceId())) {
                continue;
              }
              String instanceStateName = instance.getState().getName();
              if (!InstanceStateName.Running.toString().equals(instanceStateName)) {
                throw new BaseException(ExceptionCode.EC2INSTANCE_STATUS_PENDING);
              }
              log.info("instance network is ready");
              runningInstance.set(instance);
            }
          }
        });

    return runningInstance.get();
  }

  /**
   * createSSHKeyIfNotExisted creates the ssh keypair for ec2 login and upload it to s3 private
   * bucket for future usage.
   *
   * @param keyPairName   the keypair name used to login
   * @param bucketToStore the s3 bucket name to store the sshkey pair
   * @throws IOException   on failing to write keypair to disk
   * @throws BaseException on s3 uploading failure
   */
  public void createSSHKeyIfNotExisted(String keyPairName, String bucketToStore)
          throws IOException, BaseException {
    if (isKeyPairExisted(keyPairName)) {
      log.info("{} - ssh key pair existed", keyPairName);
      return;
    }

    try {
      // create keypair
      CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
      createKeyPairRequest.setKeyName(keyPairName);
      CreateKeyPairResult createKeyPairResult = amazonEC2.createKeyPair(createKeyPairRequest);
      String keyMaterial = createKeyPairResult.getKeyPair().getKeyMaterial();

      // store the keypair to s3 for future usage
      String keyPairFileName = keyPairName + ".pem";
      String keyPairLocalPath = "/tmp/" + keyPairFileName;
      FileUtils.writeStringToFile(new File(keyPairLocalPath), keyMaterial);
      S3Service s3Service = new S3Service(region);
      s3Service.uploadS3ObjectWithPrivateAccess(
              keyPairLocalPath, bucketToStore, keyPairFileName, false);

    } catch (AmazonEC2Exception e) {
      if (!ERROR_CODE_KEY_PAIR_ALREADY_EXIST.equals(e.getErrorCode())) {
        throw e;
      }
    }
  }

  private boolean isKeyPairExisted(String keyPairName) {
    try {
      DescribeKeyPairsRequest describeKeyPairsRequest = new DescribeKeyPairsRequest();
      describeKeyPairsRequest.setKeyNames(Collections.singletonList(keyPairName));
      DescribeKeyPairsResult describeKeyPairsResult =
              amazonEC2.describeKeyPairs(describeKeyPairsRequest);
      List<KeyPairInfo> keyPairInfoList = describeKeyPairsResult.getKeyPairs();
      if (keyPairInfoList.isEmpty()) {
        return false;
      }
    } catch (AmazonEC2Exception e) {
      if (ERROR_CODE_KEY_PAIR_NOT_FOUND.equals(e.getErrorCode())) {
        return false;
      } else {
        throw e;
      }
    }
    return true;
  }

  public void downloadSSHKey(String bucketName, String keyPairName, String toLocation) {
    s3Service.downloadS3Object(bucketName, keyPairName + ".pem", toLocation);
  }

  private String getOrCreateSecurityGroupByName(String groupName) {
    try {
      return getSecurityGroupByName(groupName);
    } catch (AmazonEC2Exception e) {
      if (ERROR_CODE_SECURITY_GROUP_NOT_FOUND.equals(e.getErrorCode())) {
        return createSecurityGroup(groupName);
      }
      throw e;
    }
  }

  private String getSecurityGroupByName(String groupName) {
    final List<SecurityGroup> securityGroups =
        amazonEC2
            .describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(groupName))
            .getSecurityGroups();
    return securityGroups.get(0).getGroupId();
  }

  /**
   * create SecurityGroup in the current aws account.
   *
   * @param groupName security group name
   * @return the created security group id
   */
  public String createSecurityGroup(String groupName) {
    try {
      // get the vpcId of default, it is always there and cannot be deleted.
      List<SecurityGroup> securityGroups =
          amazonEC2
              .describeSecurityGroups(
                  new DescribeSecurityGroupsRequest()
                      .withGroupNames(GenericConstants.DEFAULT_SECURITY_GROUP_NAME.getVal()))
              .getSecurityGroups();
      if (securityGroups.size() <= 0) {
        throw new RuntimeException("Cannot get the default security group.");
      }
      String vpcId = securityGroups.get(0).getVpcId();

      // create new security group and get the group id
      final String groupId =
              amazonEC2
                      .createSecurityGroup(
                              new CreateSecurityGroupRequest()
                                      .withGroupName(groupName)
                                      .withDescription(groupName + " used for aoc integration test")
                                      .withVpcId(vpcId))
                      .getGroupId();

      // add the incoming ip request permission
      IpPermission sshIpPermission = new IpPermission();
      sshIpPermission
              .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0"))
              .withIpProtocol("tcp")
              .withFromPort(22)
              .withToPort(22);

      IpPermission rdpIpPermission = new IpPermission();
      rdpIpPermission
              .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0"))
              .withIpProtocol("tcp")
              .withFromPort(3389)
              .withToPort(3389);

      // limit the oltp access within private
      IpPermission oltpIpPermission = new IpPermission();
      rdpIpPermission
              .withIpv4Ranges(new IpRange().withCidrIp("172.16.0.0/12"))
              .withIpProtocol("tcp")
              .withFromPort(Integer.valueOf(GenericConstants.AOC_PORT.getVal()))
              .withToPort(Integer.valueOf(GenericConstants.AOC_PORT.getVal()));

      amazonEC2.authorizeSecurityGroupIngress(
              new AuthorizeSecurityGroupIngressRequest()
                      .withGroupId(groupId)
                      .withIpPermissions(sshIpPermission, rdpIpPermission, oltpIpPermission));

      return groupId;
    } catch (AmazonEC2Exception e) {
      if (ERROR_CODE_SECURITY_GROUP_ALREADY_EXIST.equals(e.getErrorCode())) {
        return getSecurityGroupByName(groupName);
      }
      throw e;
    }
  }
}
