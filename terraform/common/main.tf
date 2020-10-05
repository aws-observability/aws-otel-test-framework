locals {
  # generate a testing_id whenever people want to use, for example, use it as a ecs cluster to prevent cluster name conflict
  testing_id = formatdate("YYYYMMDDhhmmss", timestamp())
}
