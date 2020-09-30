variable "amis" {
  default = {
    ubuntu = {
      ami_search_pattern = "ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"
      login_user = "ubuntu"
    }
    amazonlinux2 = {
      ami_search_pattern = "amzn2-ami-hvm*"
      login_user = "ec2-user"
    }
    suse = {
      ami_search_pattern = "suse-sles-15*"
      login_user = "ec2-user"
    }
    windows2019 = {
      ami_search_pattern = "Windows_Server-2019-English-Full-Base*"
      login_user = "Administrator"
    }
  }
}

data "aws_ami" "selected" {
  most_recent = true

  filter {
    name   = "name"
    values = [var.amis[var.testing_ami]["ami_search_pattern"]]
  }

  filter {
    name   = "owner-alias"
    values = ["amazon"]
  }

  owners = ["amazon"] # Canonical
}

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


