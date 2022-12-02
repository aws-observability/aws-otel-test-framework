# ------------------------------------------------------------------------
# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
# -------------------------------------------------------------------------

# create a ssh key or download ssh key from s3

## get the ssh private key
resource "tls_private_key" "ssh_key" {
  count     = var.ssh_key_name == "" ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "aws_ssh_key" {
  count      = var.ssh_key_name == "" ? 1 : 0
  key_name   = "testing-${module.common.testing_id}"
  public_key = tls_private_key.ssh_key[0].public_key_openssh
  tags = {
    "ephemeral" : "true",
    "TestCase" : var.testcase,
  }
}

## get the ssh private key
data "aws_s3_bucket_object" "ssh_private_key" {
  count  = var.ssh_key_name != "" ? 1 : 0
  bucket = var.sshkey_s3_bucket
  key    = var.sshkey_s3_private_key
}

locals {
  ssh_key_name        = var.ssh_key_name != "" ? var.ssh_key_name : aws_key_pair.aws_ssh_key[0].key_name
  private_key_content = var.ssh_key_name != "" ? data.aws_s3_bucket_object.ssh_private_key[0].body : tls_private_key.ssh_key[0].private_key_pem
}

resource "local_file" "private_key" {
  count    = var.debug ? 1 : 0
  filename = "private_key.pem"
  content  = local.private_key_content
}

