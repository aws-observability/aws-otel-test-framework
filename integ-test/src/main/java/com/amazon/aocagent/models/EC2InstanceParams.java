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

package com.amazon.aocagent.models;

import com.amazon.aocagent.enums.Architecture;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.TagSpecification;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EC2InstanceParams {
  String amiId;
  String sshKeyName;
  String securityGrpName;
  String iamRoleName;
  String userData;
  InstanceType instanceType;
  Architecture arch;
  TagSpecification tagSpecification;
}
