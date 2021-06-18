# this file is defined in validator/src/main/resources/validations
validation_config = "ecs-container-insight-prometheus.yml"
# no need for any lb
sample_app_callable = false
# sample apps that emit ecs metrics
ecs_extra_apps = {
  jmx = {
    definition   = "jmx.json"
    service_name = "jmx"
    service_type = "replica"
    replicas     = 1
    network_mode = "bridge"
    launch_type  = "EC2"
    cpu          = 256
    memory       = 256
  }

  # NOTE: for awsvpc to work form prometheus, we need change security group
  # to allow all traffic within the VPC
  jmxawsvpc = {
    definition   = "jmx.json"
    service_name = "jmxawsvpc"
    service_type = "replica"
    replicas     = 1
    network_mode = "awsvpc"
    launch_type  = "EC2"
    cpu          = 256
    memory       = 256
  }

  jmxfargate = {
    definition   = "jmx.json"
    service_name = "jmxfargate"
    service_type = "replica"
    replicas     = 1
    network_mode = "awsvpc"
    launch_type  = "FARGATE"
    cpu          = 256
    # Must set cpu and memory for fargate in specific ways
    # https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ecs-taskdefinition.html#:~:text=ContainerDefinition-,If%20your%20tasks%20will,cpu%20parameter,-512
    memory = 512
  }

  nginx = {
    definition   = "nginx.json"
    service_name = "nginx-service"
    service_type = "replica"
    replicas     = 1
    network_mode = "bridge"
    launch_type  = "EC2"
    cpu          = 384
    memory       = 384
  }
}