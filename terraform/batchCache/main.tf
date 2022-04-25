
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.8.0"
    }
  }
  # for dev env account users will need to manually 
  # edit their bucket names because variables are not allowed
  # there may be a better way to do this in the future
  # comment out the backend if you do not want to persist data
  # in s3 bucket
  backend "s3" {
    bucket         = "setup-remote-state-s3-bucket"
    dynamodb_table = "setup-remote-state-dynamodb-table"
    key            = "batch-successful-cache-terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
  }
}

provider "aws" {
  region = var.region
}

resource "aws_dynamodb_table" "batch_successful_cache" {
  name         = "BatchTestCache${var.bucketUUID}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "TestId"
  range_key    = "aoc_version"

  attribute {
    name = "TestId"
    type = "S"
  }

  attribute {
    name = "aoc_version"
    type = "S"
  }


  ttl {
    attribute_name = "TimeToExist"
    enabled        = true
  }

}
