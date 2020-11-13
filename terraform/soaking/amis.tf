variable "ami_family" {
  default = {
    amazon_linux = {
      login_user = "ec2-user"
      install_package = "aws-otel-collector.rpm"
      instance_type = "m5.2xlarge"
      otconfig_destination = "/tmp/ot-default.yml"
      download_command_pattern = "wget %s"
      install_command = "sudo rpm -Uvh aws-otel-collector.rpm"
      start_command = "sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -c /tmp/ot-default.yml -a start"
      connection_type = "ssh"
      user_data = ""
      soaking_cwagent_config = "../templates/cwagent-config/soaking-linux.json.tpl"
      soaking_cwagent_config_destination = "/tmp/cwagent-config.json"
      cwagent_download_command = "sudo rpm -Uvh https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm"
      cwagent_install_command = "echo 'donothing'"
      cwagent_start_command = "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -c file:/tmp/cwagent-config.json -s"
      soaking_cpu_metric_name = "procstat_cpu_usage"
      soaking_mem_metric_name = "procstat_memory_rss"
      soaking_process_name = "aws-otel-collector"
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
      soaking_cwagent_config = "../templates/cwagent-config/soaking-linux.json.tpl"
      soaking_cwagent_config_destination = "C:\\cwagent-config.json"
      cwagent_download_command = "powershell -command \"Invoke-WebRequest -Uri https://s3.amazonaws.com/amazoncloudwatch-agent/windows/amd64/latest/amazon-cloudwatch-agent.msi -OutFile C:\\amazon-cloudwatch-agent.msi\""
      cwagent_install_command = "msiexec /i C:\\amazon-cloudwatch-agent.msi"
      cwagent_start_command = "powershell \"& 'C:\\Program Files\\Amazon\\AmazonCloudWatchAgent\\amazon-cloudwatch-agent-ctl.ps1' -a fetch-config -c file:C:\\cwagent-config.json -s\""
      soaking_cpu_metric_name = "procstat cpu_usage"
      soaking_mem_metric_name = "procstat memory_rss"
      soaking_process_name = ".aws-otel-collector.exe"
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
    soaking_windows = {
      ami_search_pattern = "Windows_Server-2019-English-Full-Base-*"
      ami_owner = "amazon"
      family = "windows"
      arch = "amd64"
    }
    soaking_linux = {
      ami_search_pattern = "amzn2-ami-hvm*"
      ami_owner = "amazon"
      family = "amazon_linux"
      arch = "amd64"
    }
  }
}