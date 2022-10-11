### AWS OTel Collector Load Test Generator

### Parameters

1. Rate
    1. --rate, -r  (eg, 1,000, 10,000 data points per second)
    
2. Metric Flush Interval
    1. --flushInterval, -f (eg, 10s)
    
3. Collector Receiver Endpoint
    1. --url, -u
    
4. Data Format
    1. --dataFormat -d (eg, otlp, prometheus, xray)

### OTLP Metrics Load Test Sample Command,

The following is a list of optional command line flags for configuration of OTLP metrics:
* `--flushInterval, -f` : (default=1000)the metric collection export interval
* `--metric_count, -m`: (default=1) the number of metrics that should be created for each metric type
* `--datapointCount, -dp`: (default=1) the number of datapoints that should be created for each metric.
* `--observationInterval, -o`: (default=1000) the interval at which metric observations/values are updated.
* `--metricType, -mt`: (default=counter) Specify the type of metric - counter or gauge.

```
./gradlew :load-generator:run --args="metric -m=100 -dp=10 -u=localhost:4317 -d=otlp -f=1000 -mt=counter"
```

### OTLP Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=localhost:4317 -d=otlp"
```

### X-Ray Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=localhost:4317 -d=xray"
```

### StatsD Metrics Load Test Sample Command,
```
./gradlew :load-generator:run --args="metric -r=100 -u=localhost:8125 -d=statsd"
```

### Zipkin Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:9411 -d=zipkin"
```

### Jaeger Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:14268 -d=jaeger"
```