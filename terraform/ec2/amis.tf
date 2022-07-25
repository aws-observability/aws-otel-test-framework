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
    ubuntu16 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"
      ami_owner          = "099720109477"
      ami_id             = "ami-0688ba7eeeeefe3cd"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    ubuntu18 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server*"
      ami_owner          = "099720109477"
      ami_id             = "ami-02da34c96f69d525c"
      ami_product_code   = []
      family             = "debian"
      arch               = "amd64"
      login_user         = "ubuntu"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
    debian10 = {
      os_family          = "debian"
      ami_search_pattern = "debian-10-amd64*"
      ami_owner          = "679593333241"
      ami_id             = "ami-0900b247bf638c13f"
      # NOTE: we need product code to pick the right debian 10.
      ami_product_code = [
      "auhljmclkudu651zy27rih2x2"]
      family     = "debian"
      arch       = "amd64"
      login_user = "admin"
      user_data  = <<EOF
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
    debian9 = {
      os_family          = "debian"
      ami_search_pattern = "debian-stretch-hvm-x86_64-gp2*"
      ami_owner          = "679593333241"
      ami_id             = "ami-028ee8676d656d1d6"
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
    amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn2-ami-hvm-2.0.????????.?-x86_64-gp2"
      ami_owner          = "amazon"
      ami_id             = "ami-0d08ef957f0e4722b"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      login_user         = "ec2-user"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    windows2019 = {
      os_family          = "windows"
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner          = "amazon"
      ami_id             = "ami-0297fbf7e83dd1209"
      ami_product_code   = []
      family             = "windows"
      arch               = "amd64"
      login_user         = "Administrator"
    }
    amazonlinux = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn-ami-hvm-????.??.?.????????-x86_64-gp2"
      ami_owner          = "amazon"
      ami_id             = "ami-08d489468314a58df"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      login_user         = "ec2-user"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
sudo restart amazon-ssm-agent
EOF
    }
    suse15 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-15*"
      ami_owner          = "amazon"
      ami_id             = "ami-0164f097bf264d944"
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
    suse12 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-12*"
      ami_owner          = "amazon"
      ami_product_code   = []
      ami_id             = "ami-088a78f435ef7f78a"
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
    redhat8 = {
      os_family          = "redhat"
      ami_search_pattern = "RHEL-8.0.0_HVM*"
      ami_owner          = "309956199498"
      ami_id             = "ami-087c2c50437d0b80d"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      user_data          = <<EOF
#! /bin/bash
sudo dnf install -y python3
sudo dnf install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    redhat7 = {
      os_family          = "redhat"
      ami_search_pattern = "RHEL-7.7_HVM*"
      ami_owner          = "309956199498"
      ami_id             = "ami-0c2dfd42fa1fbb52c"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y python3
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    centos7 = {
      login_user         = "centos"
      os_family          = "centos"
      ami_search_pattern = "CentOS Linux 7 x86_64*"
      ami_owner          = "679593333241"
      ami_id             = "ami-0bc06212a56393ee1"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm
EOF
    }
    #centos6 is not used in testing anymore
    centos6 = {
      login_user         = "centos"
      os_family          = "centos"
      ami_search_pattern = "CentOS Linux 6 x86_64 HVM EBS 2002*"
      ami_owner          = "679593333241"
      ami_product_code   = []
      family             = "linux"
      arch               = "amd64"
      user_data          = <<EOF
#! /bin/bash
sudo iptables -I INPUT -p tcp -m tcp --dport 4317 -j ACCEPT
sudo iptables -I INPUT -p udp -m udp --dport 55690 -j ACCEPT
sudo service iptables save
cd /tmp
sudo curl https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm -o amazon-ssm-agent.rpm
sudo rpm -ivh amazon-ssm-agent.rpm
EOF
    }


    # arm amis
    arm_amazonlinux2 = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn2-ami-hvm-2.0.????????.?-arm64-gp2"
      ami_owner          = "amazon"
      ami_id             = "ami-0d3c51ccaa76cbe2b"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
EOF
    }

    arm_suse15 = {
      os_family          = "suse"
      ami_search_pattern = "suse-sles-15-sp2-v20200721-hvm-ssd-arm64*"
      ami_owner          = "amazon"
      ami_id             = "ami-0bfc92b18fd79372c"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
cd /tmp
sudo wget https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
sudo rpm -ivh amazon-ssm-agent.rpm
EOF
    }

    arm_redhat8 = {
      os_family          = "redhat"
      ami_search_pattern = "RHEL-8.0.0_HVM-20190426-arm64*"
      ami_owner          = "309956199498"
      ami_id             = "ami-0f7a968a2c17fb48b"
      ami_product_code   = []
      family             = "linux"
      arch               = "arm64"
      instance_type      = "t4g.micro"
      user_data          = <<EOF
#! /bin/bash
sudo dnf install -y python3
sudo dnf install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
EOF
    }

    arm_redhat7 = {
      os_family          = "redhat"
      ami_search_pattern = "RHEL-7.6_HVM_GA-20181122-arm64*"
      ami_owner          = "309956199498"
      ami_id             = "ami-0e00026dd0f3688e2"
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

    arm_ubuntu18 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-arm64*"
      ami_owner          = "099720109477"
      ami_id             = "ami-0f1337c0023ea5b49"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }

    arm_ubuntu16 = {
      os_family          = "ubuntu"
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-arm64*"
      ami_owner          = "099720109477"
      ami_id             = "ami-08305dd8ab642ad8c"
      ami_product_code   = []
      family             = "debian"
      arch               = "arm64"
      instance_type      = "t4g.nano"
      user_data          = <<EOF
#! /bin/bash
sudo snap refresh amazon-ssm-agent
EOF
    }
  }
}

# Local variables only apply to aws_ami for the customized filter input
locals {
  arch = var.amis[var.testing_ami]["arch"] == "amd64" ? "x86_64" : var.amis[var.testing_ami]["arch"]
}

# this ami is used to launch the emitter instance
data "aws_ami" "amazonlinux2" {
  most_recent = true

  # https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html#finding-quick-start-ami
  filter {
    name = "name"
    values = [
    "amzn2-ami-hvm-2.0.????????.?-x86_64-gp2"]
  }

  filter {
    name = "state"
    values = [
    "available"]
  }

  owners = [
  "amazon"]
}


