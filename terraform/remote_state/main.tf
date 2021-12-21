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

#Purpose of this is uploading terraform state to bucket s3 after applying an integration test case.
#In this way,  we will able to destroy the resources created within terraform state if somehow
#terraform destroy does not work out.

module "common" {
  source = "../common"
}

provider "aws" {
  region = var.region
}

locals {
  testcase_name       = split("/", var.testcase)[2]
}

resource "aws_s3_bucket_object" "object" {
  bucket = module.common.terraform_state_s3_bucket_name
  key    = "${formatdate("YYYY-MM-DD", timestamp())}/${local.testcase_name}/${var.platform}/terraform-${var.testing_id}.tfstate"
  source = "../${var.platform}/terraform.tfstate"
}



