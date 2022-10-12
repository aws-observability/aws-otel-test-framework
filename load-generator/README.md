### AWS OTel Collector Load Test Generator

### Parameters for Metric Generation:
1. Metric Count\
   --metric_count, -m  : (default=1) the number of metrics that should be generated for each metric type. This amount of metrics will be updated and exported per period. 

2. Datapoint Count\
   --flushInterval, -f : (default=1) the number of datapoints that should be created for each metric. Number of datapoints updated/exported per period shall be equal to Metric Count * Datapoint Count.

3. Observation Interval\
   --observationInterval, -o : (default=1000 ms) the interval (in ms) at which metric observations/values are updated.

4. Collector Receiver Endpoint\
   --url, -u

5. Data Format\
   --dataFormat -d (eg, otlp, statsd)

### Parameters for Trace Generation:

1. Rate\
   --rate, -r  (eg, 1,000, 10,000 data points per second)
    
2. Collector Receiver Endpoint\
   --url, -u
    
3. Data Format\
   --dataFormat -d (eg, otlp, prometheus, xray)

### OTLP Metrics Load Test Sample Command,

The following is a list of additional optional command line arguments applicable only for configuration of OTLP metrics:
* `--flushInterval, -f` : (default=1000 ms) the metric collection export interval (in ms).
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
./gradlew :load-generator:run --args="metric -m=100 -dp=10 -u=localhost:8125 -d=statsd"
```

### Zipkin Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:9411 -d=zipkin"
```

### Jaeger Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:14268 -d=jaeger"
```