

provider "aws" {
  region = var.region
}

terraform {
  required_version = ">= 1.0.0"

  backend "s3" {
    bucket = "terraform-state-integration-test"
    key = "azure-devops/terraform.tfstate"
    region = "us-west-2"
    dynamodb_table = "terraform-state-integration-test"
    encrypt = true
  }
}



