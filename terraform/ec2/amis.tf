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

variable "ami_family" {
  default = {
    debian = {
      login_user               = "ubuntu"
      install_package          = "aws-otel-collector.deb"
      instance_type            = "c5a.large"
      otconfig_destination     = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command          = "while sudo fuser /var/cache/apt/archives/lock /var/lib/apt/lists/lock /var/lib/dpkg/lock /var/lib/dpkg/lock-frontend; do echo 'Waiting for dpkg lock...' && sleep 1; done; echo 'No dpkg lock and install collector.' && sudo dpkg -i aws-otel-collector.deb"
      start_command            = "ADOT_CONFIG_URI=$(echo -n 'CONFIGURATION_URI_PLACEHOLDER' | base64 -d)\nsudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c \"$ADOT_CONFIG_URI\" -f FEATUREGATE_PLACEHOLDER -a start"
      status_command           = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -a status"
      ssm_validate             = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -a status | grep running"
      connection_type          = "ssh"
      user_data                = ""
      wait_cloud_init          = "for i in {1..300}; do [ ! -f /var/lib/cloud/instance/boot-finished ] && echo 'Waiting for cloud-init...'$i && sleep 1 || break; done"
    }
    linux = {
      login_user               = "ec2-user"
      install_package          = "aws-otel-collector.rpm"
      instance_type            = "c5a.large"
      otconfig_destination     = "/tmp/ot-default.yml"
      download_command_pattern = "curl %s --output aws-otel-collector.rpm"
      install_command          = "sudo rpm -Uvh aws-otel-collector.rpm"
      start_command            = "ADOT_CONFIG_URI=$(echo -n 'CONFIGURATION_URI_PLACEHOLDER' | base64 -d)\nsudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c \"$ADOT_CONFIG_URI\" -f FEATUREGATE_PLACEHOLDER -a start"
      status_command           = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -a status"
      ssm_validate             = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -a status | grep running"
      connection_type          = "ssh"
      user_data                = ""
      wait_cloud_init          = "for i in {1..300}; do [ ! -f /var/lib/cloud/instance/boot-finished ] && echo 'Waiting for cloud-init...'$i && sleep 1 || break; done"
    }
    windows = {
      login_user               = "Administrator"
      install_package          = "aws-otel-collector.msi"
      instance_type            = "c5a.large"
      otconfig_destination     = "C:\\ot-default.yml"
      download_command_pattern = "powershell -command \"Invoke-WebRequest -Uri %s -OutFile C:\\aws-otel-collector.msi\""
      install_command          = "msiexec /i C:\\aws-otel-collector.msi"
      start_command            = "$url = [System.Text.Encoding]::ASCII.GetString([System.Convert]::FromBase64String('CONFIGURATION_URI_PLACEHOLDER'))\n. 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -ConfigLocation $url -FeatureGates 'FEATUREGATE_PLACEHOLDER' -Action start"
      status_command           = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -Action status\""
      ssm_validate             = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -Action status\" | findstr running"
      connection_type          = "winrm"
      user_data                = ""
      wait_cloud_init          = " "
    }
  }
}

########################################
##  define the amis to test collector ##
##
## os_family, which will be used to construct the downloading url of collector on s3
## ami_search_pattern, which will be used to search the amis from aws
## ami_owner, could be "amazon", "aws-marketplace", or a dedicated account number, for example, redhat amis are distributed by this account 309956199498(Redhat Inc)
## family, could be "linux", "debian", "windows"
## arch, which will be used to construct the downloading url of collector on s3
## login_user, which will be taken first, if null then the take from family.
########################################
variable "amis" {
  default = {
    ubuntu20 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-focal*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    arm_ubuntu20 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-focal*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "ubuntu"
      instance_type      = "c6g.large"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    ubuntu22 = {
      os_family          = "ubuntu"
      ami_search_pattern = "cloudwatch-agent-integration-test-ubuntu-LTS-22*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "ubuntu"
      user_data          = ""
    }
    arm_ubuntu22 = {
      os_family          = "ubuntu"
      ami_search_pattern = "cloudwatch-agent-integration-test-ubuntu-LTS-22-arm64*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "ubuntu"
      instance_type      = "c6g.large"
      user_data          = ""
    }
    # Debian Distribution
    debian11 = {
      os_family          = "debian"
      ami_search_pattern = "debian-11-*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "admin"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/debian_amd64/amazon-ssm-agent.deb
while sudo fuser {/var/{lib/{dpkg,apt/lists},cache/apt/archives}/lock,/var/lib/dpkg/lock-frontend}; do
   echo 'Waiting for dpkg lock...' && sleep 1
done
sudo dpkg -i amazon-ssm-agent.deb
sudo systemctl enable amazon-ssm-agent
EOF
    }
    arm_debian11 = {
      os_family          = "debian"
      ami_search_pattern = "cloudwatch-agent-integration-test-debian-11-arm64*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "admin"
      instance_type      = "c6g.large"
      user_data          = ""
    }
    debian10 = {
      os_family          = "debian"
      ami_search_pattern = "debian-10-*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "admin"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/debian_amd64/amazon-ssm-agent.deb
while sudo fuser {/var/{lib/{dpkg,apt/lists},cache/apt/archives}/lock,/var/lib/dpkg/lock-frontend}; do
   echo 'Waiting for dpkg lock...' && sleep 1
done
sudo dpkg -i amazon-ssm-agent.deb
sudo systemctl enable amazon-ssm-agent
EOF
    }
    arm_debian10 = {
      os_family          = "debian"
      ami_search_pattern = "debian-10-arm64*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "admin"
      instance_type      = "c6g.large"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/debian_arm64/amazon-ssm-agent.deb
while sudo fuser {/var/{lib/{dpkg,apt/lists},cache/apt/archives}/lock,/var/lib/dpkg/lock-frontend}; do
   echo 'Waiting for dpkg lock...' && sleep 1
done
sudo dpkg -i amazon-ssm-agent.deb
sudo systemctl enable amazon-ssm-agent
EOF
    }
    #AL3
    amazonlinux3 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "cloudwatch-agent-integration-test-x86-al2023*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      login_user         = "ec2-user"
      user_data          = ""
    }
    arm_amazonlinux3 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "cloudwatch-agent-integration-test-aarch64-al2023*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      login_user         = "ec2-user"
      instance_type      = "c6g.large"
      user_data          = ""
    }
    #AL2
    amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "cloudwatch-agent-integration-test-al2*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      login_user         = "ec2-user"
      user_data          = ""
    }
    arm_amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "cloudwatch-agent-integration-test-arm64-al2*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      login_user         = "ec2-user"
      instance_type      = "c6g.large"
      user_data          = ""
    }
    # Windows Distribution
    windows2022 = {
      os_family          = "windows"
      ami_search_pattern = "cloudwatch-agent-integration-test-win-2022*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "windows"
      arch               = "amd64"
      login_user         = "Administrator"
    }
    windows2019 = {
      os_family          = "windows"
      ami_search_pattern = "cloudwatch-agent-integration-test-win-2019*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "windows"
      arch               = "amd64"
      login_user         = "Administrator"
    }
    # Suse Distribution
    suse15 = {
      os_family          = "suse"
      ami_search_pattern = "cloudwatch-agent-integration-test-sles-15*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "amd64"
      user_data          = ""
    }
    arm_suse15 = {
      os_family          = "suse"
      ami_search_pattern = "cloudwatch-agent-integration-test-sles-15-arm64*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "arm64"
      instance_type      = "c6g.large"
      user_data          = ""
    }
    suse12 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-12-sp5-v????????-hvm-ssd-x86_64"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "amd64"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
sudo rpm -Uvh amazon-ssm-agent.rpm
sudo systemctl enable amazon-ssm-agent
sudo systemctl start amazon-ssm-agent
EOF
    }
    # Redhat Distribution
    redhat8 = {
      os_family          = "redhat"
      ami_search_pattern = "cloudwatch-agent-integration-test-rhel8-base*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      login_user         = "ec2-user"
      user_data          = ""
    }
    arm_redhat8 = {
      os_family          = "redhat"
      ami_search_pattern = "cloudwatch-agent-integration-test-rhel8-arm64*"
      ami_owner          = "506463145083"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      instance_type      = "c6g.large"
      login_user         = "ec2-user"
      user_data          = ""
    }
  }
}

# Local variables only apply to aws_ami for the customized filter input
locals {
  arch = var.amis[var.testing_ami]["arch"] == "amd64" ? "x86_64" : var.amis[var.testing_ami]["arch"]
}

data "aws_ami" "selected" {
  most_recent = true

  owners = [
  var.amis[var.testing_ami]["ami_owner"]]

  filter {
    name = "name"
    values = [
    var.amis[var.testing_ami]["ami_search_pattern"]]
  }

  filter {
    name   = "architecture"
    values = [local.arch]
  }

  filter {
    name = "state"
    values = [
    "available"]
  }

}

# this ami is used to launch the emitter instance
data "aws_ami" "amazonlinux2" {
  most_recent = true

  # https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html#finding-quick-start-ami
  filter {
    name = "name"
    values = [
    "amzn2-ami-kernel*"]
  }

  filter {
    name = "state"
    values = [
    "available"]
  }

  // sidecar instance should be fixed in x86_64 instance types
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  owners = [
  "amazon"]
}


