variable "ami_family" {
  default = {
    ubuntu = {
      login_user = "ubuntu"
      install_package = "aws-observability-collector.deb"
      instance_type = "t2.micro"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command = "sudo dpkg -i aws-observability-collector.deb"
      start_command = "sudo /opt/aws/aws-observability-collector/bin/aws-observability-collector-ctl -c /tmp/ot-default.yml -a start"
      connection_type = "ssh"
      user_data = ""
    },
    amazon_linux = {
      login_user = "ec2-user"
      install_package = "aws-observability-collector.rpm"
      instance_type = "t2.micro"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command = "sudo rpm -Uvh aws-observability-collector.rpm"
      start_command = "sudo /opt/aws/aws-observability-collector/bin/aws-observability-collector-ctl -c /tmp/ot-default.yml -a start"
      connection_type = "ssh"
      user_data = ""
    }
    windows = {
      login_user = "Administrator"
      install_package = "aws-observability-collector.msi"
      instance_type = "t2.micro"
      otconfig_destination = "C:\\ot-default.yml"
      download_command_pattern = "powershell -command \"Invoke-WebRequest -Uri %s -OutFile C:\\aws-observability-collector.msi\""
      install_command = "msiexec /i C:\\aws-observability-collector.msi"
      start_command = "powershell \"& 'C:\\Program Files\\Amazon\\AwsObservabilityCollector\\aws-observability-collector-ctl.ps1' -ConfigLocation C:\\ot-default.yml -Action start\""
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

variable "amis" {
  default = {
    ubuntu16 = {
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"
      ami_owner = "099720109477"
      family = "ubuntu"
      arch = "amd64"
    }
    amazonlinux2 = {
      ami_search_pattern = "amzn2-ami-hvm*"
      ami_owner = "amazon"
      family = "amazon_linux"
      arch = "amd64"
    }
    windows2019 = {
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner = "amazon"
      family = "windows"
      arch = "amd64"
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
data "aws_ami" "suse" {
  most_recent = true

  filter {
    name   = "owner-alias"
    values = ["amazon"]
  }

  filter {
    name   = "name"
    values = ["suse-sles-15*"]
  }

  owners = ["amazon"] # Canonical
}


