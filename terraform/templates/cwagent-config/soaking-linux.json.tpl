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
            "commit_id": "${commit_id}",
            "testcase": "${testcase}",
            "testing_ami": "${testing_ami}",
            "launch_date": "${launch_date}",
            "negative_soaking": "${negative_soaking}",
            "data_rate": "${data_rate}",
            "instance_type": "${instance_type}"
          }
        }
      ]
    },
    "namespace": "${soaking_metric_namespace}"
  }
}