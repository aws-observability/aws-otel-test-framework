variable "ami_family" {
  default = {
    amazon_linux = {
      login_user = "ec2-user"
      install_package = "aws-otel-collector.rpm"
      instance_type = "m5.2xlarge"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command = "sudo rpm -Uvh aws-otel-collector.rpm"
      set_env_var_command = "sudo chmod 777 /opt/aws/aws-otel-collector/etc/.env && sudo echo 'SAMPLE_APP_HOST=%s' >> /opt/aws/aws-otel-collector/etc/.env && sudo echo 'SAMPLE_APP_PORT=%s' >> /opt/aws/aws-otel-collector/etc/.env"
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
      set_env_var_command = "powershell \"[System.Environment]::SetEnvironmentVariable('SAMPLE_APP_HOST', '%s', [System.EnvironmentVariableTarget]::Machine); [System.Environment]::SetEnvironmentVariable('SAMPLE_APP_PORT', '%s', [System.EnvironmentVariableTarget]::Machine)\""
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

variable "amis" {
  default = {
    canary_windows = {
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner = "amazon"
      family = "windows"
      arch = "amd64"
    }
    canary_linux = {
      ami_search_pattern = "amzn2-ami-hvm*"
      ami_owner = "amazon"
      family = "amazon_linux"
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
    values = ["suse-sles-15-sp1-v20200501-hvm-ssd-x86_64"]
  }

  owners = ["amazon"] # Canonical
}
