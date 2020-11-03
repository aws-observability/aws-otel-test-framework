## AWS OTel Collector Load Test Generator

### Parameters

1. Rate
    1. --rate, -r  (eg, 1,000, 10,000 data points per second)
    
2. Metric Flush Interval
    1. --flushInterval, -f (eg, 10s)
    
3. Collector Receiver Endpoint
    1. --url, -u
    
4. Data Format
    1. --dataFormat -d (eg, otlp, prometheus, xray)

### Metrics Load Test Sample Command,
```
./gradlew :load-generator:run --args="metric -r=10000 -u=localhost:55680 -d=otlp -f=10000"
```