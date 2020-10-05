variable "region" {
  default = "us-west-2"
}

variable "sshkey_s3_bucket" {
  description = "please provide the bucket name here to store the ssh private key, it could be failing as s3 bucket name is unique globally"
}