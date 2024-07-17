### AWS OTel Collector Load Test Generator

### Parameters for Metric Generation:
1. Rate\
   --rate, -r  (eg, 1,000, 10,000 metric counts  per second) : (default=1) the number of metrics that should be generated for each metric type. This amount of metrics will be updated/exported per period. 

2. Collector Receiver Endpoint\
   --url, -u

3. Data Format\
   --dataFormat -d (eg, otlp, statsd)

4. Metric Type
  --metricType, -mt : (default=counter) Specify the type of metric - counter or gauge.

### Parameters for Trace Generation:

1. Rate\
   --rate, -r  (eg, 1,000, 10,000 data points per second)
    
2. Collector Receiver Endpoint\
   --url, -u
    
3. Data Format\
   --dataFormat -d (eg, otlp, prometheus, xray)

### OTLP Metrics Load Test Sample Command,
```
./gradlew :load-generator:run --args="metric -r=100 -u=localhost:4317 --metricType=counter -d=otlp"
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
./gradlew :load-generator:run --args="metric -r=100 -u=localhost:8125 --metricType=counter -d=statsd"
```

### Zipkin Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:9411 -d=zipkin"
```

### Jaeger Trace Load Test Sample Command,
```
./gradlew :load-generator:run --args="trace -r=100 -u=http://localhost:14268 -d=jaeger"
```
