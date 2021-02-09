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
      login_user = "ubuntu"
      install_package = "aws-otel-collector.deb"
      instance_type = "t2.micro"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command = "sudo dpkg -i aws-otel-collector.deb"
      start_command = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      connection_type = "ssh"
      user_data = ""
    }
    linux = {
      login_user = "ec2-user"
      install_package = "aws-otel-collector.rpm"
      instance_type = "t2.micro"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "curl %s --output aws-otel-collector.rpm"
      install_command = "sudo rpm -Uvh aws-otel-collector.rpm"
      start_command = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      connection_type = "ssh"
      user_data = ""
    }
    windows = {
      login_user = "Administrator"
      install_package = "aws-otel-collector.msi"
      instance_type = "t3.medium"
      otconfig_destination = "C:\\ot-default.yml"
      download_command_pattern = "powershell -command \"Invoke-WebRequest -Uri %s -OutFile C:\\aws-otel-collector.msi\""
      install_command = "msiexec /i C:\\aws-otel-collector.msi"
      start_command = "powershell \"& 'C:\\Program Files\\Amazon\\AwsOtelCollector\\aws-otel-collector-ctl.ps1' -ConfigLocation C:\\ot-default.yml -Action start\""
      connection_type = "winrm"
      user_data = <<EOF
<powershell>
winrm quickconfig -q
winrm set winrm/config/winrs '@{MaxMemoryPerShellMB="300"}'
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
    ubuntu16 = {
      os_family = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"
      ami_owner = "099720109477"
      family = "debian"
      arch = "amd64"
      login_user = "ubuntu"
    }
    ubuntu18 = {
      os_family = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server*"
      ami_owner = "099720109477"
      family = "debian"
      arch = "amd64"
      login_user = "ubuntu"
    }
    debian10 = {
      os_family = "debian"
      ami_search_pattern = "debian-10-amd64*"
      ami_owner = "679593333241"
      family = "debian"
      arch = "amd64"
      login_user = "admin"
    }
    debian9 = {
      os_family = "debian"
      ami_search_pattern = "debian-stretch-hvm-x86_64-gp2*"
      ami_owner = "679593333241"
      family = "debian"
      arch = "amd64"
      login_user = "admin"
    }
    amazonlinux2 = {
      os_family = "amazon_linux"
      ami_search_pattern = "amzn2-ami-hvm*"
      ami_owner = "amazon"
      family = "linux"
      arch = "amd64"
      login_user = "ec2-user"
    }
    windows2019 = {
      os_family = "windows"
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner = "amazon"
      family = "windows"
      arch = "amd64"
      login_user = "Administrator"
    }
    amazonlinux = {
      os_family = "amazon_linux"
      ami_search_pattern = "amzn-ami-hvm*"
      ami_owner = "amazon"
      family = "linux"
      arch = "amd64"
      login_user = "ec2-user"
    }
    suse15 = {
      os_family = "suse"
      ami_search_pattern = "suse-sles-15*"
      family = "linux"
      login_user = "ec2-user"
      arch = "amd64"
      ami_owner = "amazon"
    }
    suse12 = {
      os_family = "suse"
      ami_search_pattern = "suse-sles-12*"
      family = "linux"
      login_user = "ec2-user"
      arch = "amd64"
      ami_owner = "amazon"
    }
    redhat8 = {
      os_family = "redhat"
      ami_search_pattern = "RHEL-8.0.0_HVM*"
      family = "linux"
      ami_owner = "309956199498"
      arch = "amd64"
    }
    redhat7 = {
      os_family = "redhat"
      ami_search_pattern = "RHEL-7.7_HVM*"
      family = "linux"
      ami_owner = "309956199498"
      arch = "amd64"
    }
    centos7 = {
      login_user = "centos"
      os_family = "centos"
      ami_search_pattern = "CentOS Linux 7 x86_64*"
      family = "linux"
      ami_owner = "679593333241"
      arch = "amd64"
    }
    centos6 = {
      login_user = "centos"
      os_family = "centos"
      ami_search_pattern = "CentOS Linux 6 x86_64 HVM EBS 2002*"
      family = "linux"
      ami_owner = "679593333241"
      arch = "amd64"
      user_data = <<EOF
#! /bin/bash
sudo iptables -I INPUT -p tcp -m tcp --dport 4317 -j ACCEPT
sudo iptables -I INPUT -p udp -m udp --dport 55690 -j ACCEPT
sudo service iptables save
EOF
    }


    # arm amis
    arm_amazonlinux2 = {
      os_family = "amazon_linux"
      ami_search_pattern = "amzn2-ami-hvm-2.0.20200722.0-arm64*"
      family = "linux"
      ami_owner = "amazon"
      arch = "arm64"
      instance_type = "t4g.nano"
    }

    arm_suse15 = {
      os_family = "suse"
      ami_search_pattern = "suse-sles-15-sp2-v20200721-hvm-ssd-arm64*"
      family = "linux"
      ami_owner = "amazon"
      arch = "arm64"
      instance_type = "t4g.nano"
    }

    arm_redhat8 = {
      os_family = "redhat"
      ami_search_pattern = "RHEL-8.0.0_HVM-20190426-arm64*"
      family = "linux"
      ami_owner = "309956199498"
      arch = "arm64"
      instance_type = "t4g.micro"
    }

    arm_redhat7 = {
      os_family = "redhat"
      ami_search_pattern = "RHEL-7.6_HVM_GA-20181122-arm64*"
      family = "linux"
      ami_owner = "309956199498"
      arch = "arm64"
      instance_type = "t4g.micro"
    }

    arm_ubuntu18 = {
      os_family = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-arm64*"
      family = "debian"
      ami_owner = "099720109477"
      arch = "arm64"
      instance_type = "t4g.nano"
    }

    arm_ubuntu16 = {
      os_family = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-arm64*"
      family = "debian"
      ami_owner = "099720109477"
      arch = "arm64"
      instance_type = "t4g.nano"
    }



  }
}

data "aws_ami" "selected" {
  most_recent = true

  filter {
    name   = "name"
    values = [var.amis[var.testing_ami]["ami_search_pattern"]]
  }

  owners = [var.amis[var.testing_ami]["ami_owner"]] # Canonical
}


# this ami is used to launch the emitter instance
data "aws_ami" "amazonlinux2" {
  most_recent = true

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm*"]
  }

  owners = ["amazon"] # Canonical
}


