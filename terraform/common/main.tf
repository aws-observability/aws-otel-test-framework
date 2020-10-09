# generate a testing_id whenever people want to use, for example, use it as a ecs cluster to prevent cluster name conflict
resource "random_id" "testing_id" {
  byte_length = 8
}

