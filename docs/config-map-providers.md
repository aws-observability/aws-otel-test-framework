# Testing config map providers

Config map provider is a type of collector component that is responsible by
fetching configuration from a URI and proving that to the collector. The
OpenTelemetry collector is extensible enough to support different types of
config map providers that live in the opentelemetry-collector and opentelemetry-collector-contrib
repositories.

This document describes how we are testing the following config map providers:
* http
* https
* s3

## Test procedure

The test procedure for config map providers consists in writing a configuration
file to s3 with with a metrics pipeline containing a metricstransform processor. The metricstransform processor is
used to inject a dimension whose value is a unique id that is hard coded in the file. The reason why we use this
unique id is because we can confirm that this value can be only be obtained from the configuration file.

S3 is used for testing the s3, http and https config map providers, because it allows you access the data in s3 through these
protocols.

```
+------------------+              +-----------+
|                  +-------------->           |
|  Test Framework  |              |           |
|                  |              |  S3       |
+------------------+              |  Bucket   |
                                  +-----+-----+
                                        |
                                        |   +------------+
                                        |   |            |
                                        |   | Config     |
                                        |   | Unique Id  |
                                        |   +------------+
                                        v
               +-----------+       +----+----------+             +--------------------+
               |           |       |               |             |                    |
               | SampleApp +------->    Collector  +-------------> Cloudwatch Metrics |
               |           |       |               |             |                    |
               +----^------+       +---------------+             +----------+---------+
                    ^                                                       |
                    |                                                       |
                    |                                                       |
                    |                                                       |
                    |                +-----------------+                    |
                    |                |                 |                    |
                    +----------------+     Validator   <--------------------+
                                     |                 |
                                     +-----------------+
```

```yaml
  metricstransform:
    transforms:
      - include: ".*${testing_id}"
        match_type: regexp
        action: update
        operations:
          - action: add_label
            new_label: "testingId"
            new_value: "${testing_id}"
```

## References
* https://github.com/open-telemetry/opentelemetry-collector/tree/main/confmap
* https://docs.aws.amazon.com/AmazonS3/latest/userguide/ShareObjectPreSignedURL.html
