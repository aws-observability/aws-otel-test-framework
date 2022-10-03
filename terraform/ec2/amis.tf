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
      instance_type            = "t2.micro"
      otconfig_destination     = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command          = "while sudo fuser /var/cache/apt/archives/lock /var/lib/apt/lists/lock /var/lib/dpkg/lock /var/lib/dpkg/lock-frontend; do echo 'Waiting for dpkg lock...' && sleep 1; done; echo 'No dpkg lock and install collector.' && sudo dpkg -i aws-otel-collector.deb"
      start_command            = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      status_command           = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status"
      ssm_validate             = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status | grep running"
      connection_type          = "ssh"
      user_data                = ""
      wait_cloud_init          = "for i in {1..300}; do [ ! -f /var/lib/cloud/instance/boot-finished ] && echo 'Waiting for cloud-init...'$i && sleep 1 || break; done"
    }
    linux = {
      login_user               = "ec2-user"
      install_package          = "aws-otel-collector.rpm"
      instance_type            = "t2.micro"
      otconfig_destination     = "/tmp/ot-default.yml"
      download_command_pattern = "curl %s --output aws-otel-collector.rpm"
      install_command          = "sudo rpm -Uvh aws-otel-collector.rpm"
      start_command            = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      status_command           = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status"
      ssm_validate             = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status | grep running"
      connection_type          = "ssh"
      user_data                = ""
      wait_cloud_init          = "for i in {1..300}; do [ ! -f /var/lib/cloud/instance/boot-finished ] && echo 'Waiting for cloud-init...'$i && sleep 1 || break; done"
    }
    windows = {
      login_user               = "Administrator"
      install_package          = "aws-otel-collector.msi"
      instance_type            = "t3.medium"
      otconfig_destination     = "C:\\ot-default.yml"
      download_command_pattern = "powershell -command \"Invoke-WebRequest -Uri %s -OutFile C:\\aws-otel-collector.msi\""
      install_command          = "msiexec /i C:\\aws-otel-collector.msi"
      start_command            = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -ConfigLocation C:\\ot-default.yml -Action start\""
      status_command           = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -ConfigLocation C:\\ot-default.yml -Action status\""
      ssm_validate             = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -ConfigLocation C:\\ot-default.yml -Action status\" | findstr running"
      connection_type          = "winrm"
      user_data                = <<EOF
<powershell>
winrm quickconfig -q
winrm set winrm/config/winrs '@{MaxShellsPerUser="100"}'
winrm set winrm/config/winrs '@{MaxConcurrentUsers="30"}'
winrm set winrm/config/winrs '@{MaxProcessesPerShell="100"}'
winrm set winrm/config/winrs '@{MaxMemoryPerShellMB="1024"}'
winrm set winrm/config '@{MaxTimeoutms="1800000"}'
winrm set winrm/config/service '@{AllowUnencrypted="true"}'
winrm set winrm/config/service/auth '@{Basic="true"}'
netsh advfirewall firewall add rule name="WinRM 5985" protocol=TCP dir=in localport=5985 action=allow
netsh advfirewall firewall add rule name="WinRM 5986" protocol=TCP dir=in localport=5986 action=allow
net stop winrm
sc.exe config winrm start=auto
net start winrm
Set-NetFirewallProfile -Profile Public -Enabled False
</powershell>
EOF
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
    # Ubuntu Distribution
    ubuntu18 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-*"
      owners             = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "x86_64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    arm_ubuntu18 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-*"
      owners             = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "ubuntu"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    ubuntu20 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-focal*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "x86_64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    arm_ubuntu20 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-focal*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "ubuntu"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    ubuntu22 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-jammy*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "x86_64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    arm_ubuntu22 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-jammy*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "ubuntu"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    # Debian Distribution
    debian11 = {
      os_family          = "debian"
      ami_search_pattern = "debian-11-*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "x86_64"
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
      ami_search_pattern = "debian-11-*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "admin"
      instance_type      = "t4g.nano"
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
    debian10 = {
      os_family          = "debian"
      ami_search_pattern = "debian-10-*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "x86_64"
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
      ami_search_pattern = "debian-10-*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      login_user         = "admin"
      instance_type      = "t4g.nano"
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
    #AL2
    amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn2-ami-kernel*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      arch               = "x86_64"
      login_user         = "ec2-user"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    arm_amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn2-ami-kernel*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      login_user         = "ec2-user"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
EOF
    }
    # Windows Distribution
    windows2022 = {
      os_family          = "windows"
      ami_search_pattern = "Windows_Server-2022-English-Full-Base*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "windows"
      arch               = "x86_64"
      login_user         = "Administrator"
    }
    windows2019 = {
      os_family          = "windows"
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner          = "amazon"
      ami_id             = "ami-0297fbf7e83dd1209"
      ami_product_code   = []
      family             = "windows"
      arch               = "x86_64"
      login_user         = "Administrator"
    }
    # Suse Distribution
    suse15 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-15*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "x86_64"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
sudo rpm -Uvh amazon-ssm-agent.rpm
sudo systemctl enable amazon-ssm-agent
sudo systemctl start amazon-ssm-agent
EOF
    }
    arm_suse15 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-15*"
      ami_owner          = "aws-marketplace"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "arm64"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
sudo rpm -ivh amazon-ssm-agent.rpm
EOF
    }
    suse12 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-12*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      login_user         = "ec2-user"
      arch               = "x86_64"
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
      ami_search_pattern = "RHEL-8.6.0_HVM*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      arch               = "x86_64"
      user_data          = <<EOF
#! /bin/bash
sudo dnf install -y python3
sudo dnf install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    arm_redhat8 = {
      os_family          = "redhat"
      ami_search_pattern = "RHEL-8.6.0_HVM*"
      ami_owner          = "amazon"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      instance_type      = "t4g.micro"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y python3
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
EOF
    }
  }
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
    values = [var.amis[var.testing_ami]["arch"]]
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

  owners = [
  "amazon"]
}


