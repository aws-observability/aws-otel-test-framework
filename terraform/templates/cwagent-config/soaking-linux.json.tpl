{
  "agent": {
    "metrics_collection_interval": 10
  },
  "metrics": {
    "append_dimensions": {
      "InstanceId": "$${aws:InstanceId}"
    },
    "metrics_collected": {
      "procstat": [
        {
          "measurement": [
            "cpu_usage",
            "memory_rss"
          ],
          "exe": "aws-otel-collector",
          "append_dimensions": {
            "testing_id": "${testing_id}",
            "testcase": "${testcase}",
            "testing_ami": "${testing_ami}"
          }
        }
      ]
    },
    "namespace": "${soaking_metric_namespace}"
  }
}