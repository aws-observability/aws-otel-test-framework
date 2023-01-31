
receivers:
  hostmetrics:
    collection_interval: 15s
    scrapers:
      cpu:
        metrics:
          system.cpu.utilization:
            enabled: true
      load:
      memory:
        metrics:
          system.memory.utilization:
            enabled: true
      disk:
      filesystem:
        metrics:
          system.filesystem.utilization:
            enabled: true
      network:
        metrics:
          system.network.conntrack.count:
            enabled: true
          system.network.conntrack.max:
            enabled: true
      paging:
        metrics:
          system.paging.utilization:
            enabled: true
      processes:
      process:
        metrics:
          process.memory.physical_usage:
            enabled: false
          process.memory.virtual_usage:
            enabled: false
          process.memory.usage:
            enabled: true
          process.memory.virtual:
            enabled: true
          process.context_switches:
            enabled: true
          process.cpu.utilization:
            enabled: true
          process.memory.utilization:
            enabled: true
          process.open_file_descriptors:
            enabled: true
          process.paging.faults:
            enabled: true
          process.signals_pending:
            enabled: true
          process.threads:
            enabled: true

processors:
  resourcedetection/ec2:
    detectors: [ec2]
    timeout: 2s
    attributes: [host.id]
    override: true

exporters:
  awsemf:
    namespace: '${testing_id}'
    region: '${region}'
    dimension_rollup_option: NoDimensionRollup
    resource_to_telemetry_conversion:
      enabled: true

service:
  pipelines:
    metrics:
      receivers: [hostmetrics]
      processors: [resourcedetection/ec2]
      exporters: [awsemf]