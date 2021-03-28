# Sample JMX traffic with Tomcat

This a sample application that generates jmx metrics and export in prometheus format based
on  [container insight sample](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights-Prometheus-Sample-Workloads-ECS-javajmx.html)
.

## Usage

```bash
# Build the image
docker build -t prometheus-sample-tomcat-jmx .

# http://localhost:8080/samplejmx/ hello world
# http://localhost:9404/metrics (actually any path works)
docker run --rm -p 8080:8080 -p 9404:9404 prometheus-sample-tomcat-jmx
```

## Deployment

ECS TaskDefinition

```json
{
  "family": "aoc-ecs-sd-tomcat-jmx",
  "taskRoleArn": "{{task-role-arn}}",
  "executionRoleArn": "{{execution-role-arn}}",
  "networkMode": "bridge",
  "containerDefinitions": [
    {
      "name": "tomcat-prometheus-workload-java-ec2-bridge-dynamic-port",
      "image": "prometheus-sample-tomcat-jmx:0.1",
      "portMappings": [
        {
          "protocol": "tcp",
          "containerPort": 9404
        }
      ],
      "dockerLabels": {
        "ECS_PROMETHEUS_EXPORTER_PORT": "9404",
        "Java_EMF_Metrics": "true"
      }
    }
  ],
  "requiresCompatibilities": [
    "EC2"
  ],
  "cpu": "256",
  "memory": "512"
}
```

## References

How to create a war file

- [War file http url is same as file name](https://stackoverflow.com/questions/5109112/how-to-deploy-a-war-file-in-tomcat-7)
- [Create web app in IDEA](https://www.jetbrains.com/help/idea/deploying-a-web-app-into-an-app-server-container.html)
- [Maven war](https://maven.apache.org/plugins/maven-war-plugin/usage.html) `mvn package`