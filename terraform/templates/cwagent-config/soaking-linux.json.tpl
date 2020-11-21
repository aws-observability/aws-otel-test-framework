{
  "agent": {
    "metrics_collection_interval": 10
  },
  "metrics": {
    "append_dimensions": {
      "InstanceId": "$${aws:InstanceId}"
    },
    "metrics_collected": {
      "netstat": {
        "measurement": [
            "tcp_time_wait",
            "tcp_established",
            "tcp_close_wait"
        ]
      },
      "procstat": [
        {
          "measurement": [
            "cpu_usage",
            "memory_rss"
          ],
          "exe": "aws-otel-collector",
          "append_dimensions": {
            "TestingId": "${testing_id}"
          }
        }
      ]
    },
    "namespace": "${soaking_metric_namespace}"
  }
}