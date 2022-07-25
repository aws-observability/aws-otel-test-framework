variable "ami_family" {
  default = {
    amazon_linux = {
      login_user               = "ec2-user"
      install_package          = "aws-otel-collector.rpm"
      instance_type            = "t3.small"
      otconfig_destination     = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command          = "sudo rpm -Uvh aws-otel-collector.rpm"
      start_command            = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      status_command           = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status"
      ssm_validate             = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a status | grep running"
      connection_type          = "ssh"
      user_data                = ""
      wait_cloud_init          = "for i in {1..60}; do [ ! -f /var/lib/cloud/instance/boot-finished ] && echo 'Waiting for cloud-init...' && sleep 1 || break; done"
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

variable "amis" {
  default = {
    canary_windows = {
      os_family          = "windows"
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner          = "amazon"
      ami_id             = "ami-0297fbf7e83dd1209"
      ami_product_code   = []
      family             = "windows"
      arch               = "amd64"
    }
    canary_linux = {
      os_family          = "amazon_linux"
      ami_search_pattern = "amzn2-ami-hvm-2.0.????????.?-x86_64-gp2"
      ami_owner          = "amazon"
      ami_id             = "ami-0d08ef957f0e4722b"
      ami_product_code   = []
      family             = "amazon_linux"
      arch               = "amd64"
    }
  }
}

