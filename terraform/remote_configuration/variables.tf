// Bucket prefix
variable "configuration_bucket" {
    default = "adot-collector-integ-test-configurations"
}

// Content of the configuration to be stored in s3
variable "content" {
    type = string
}

// The scheme that will be used in the url returned by this module
variable "scheme" {
    type = string
}

// Unique id, used to name the objects stored into s3
variable "testing_id" {
    type = string
}
