{
  "agent": {
    "metrics_collection_interval": 10
  },
  "metrics": {
    "append_dimensions": {
      "InstanceId": "$${aws:InstanceId}"
    },
    "metrics_collected": {
      "cpu": {
        "measurement": [
          "cpu_usage_idle",
          "cpu_usage_iowait",
          "cpu_usage_user",
          "cpu_usage_system"
        ],
        "totalcpu": false
      },
      "disk": {
        "measurement": [
          "used_percent",
          "inodes_free"
        ]
      },
      "diskio": {
        "measurement": [
          "io_time"
        ]
      },
      "mem": {
        "measurement": [
          "mem_used_percent"
        ]
      },
      "statsd": {
      },
      "swap": {
        "measurement": [
          "swap_used_percent"
        ]
      },
      "procstat": [
        {
          "measurement": [
            "cpu_usage",
            "memory_rss"
          ],
          "exe": "aws-otel-collector"
        }
      ]
    },
    "namespace": "${soaking_metric_namespace}"
  }
}