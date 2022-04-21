
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.8.0"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_dynamodb_table" "batch_successful_cache" {
  name         = "BatchTestCache${var.bucketUUID}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "TestId"

  attribute {
    name = "TestId"
    type = "S"
  }


  ttl {
    attribute_name = "TimeToExist"
    enabled        = true
  }

}