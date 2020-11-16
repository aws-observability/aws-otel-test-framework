## Build Sample App

For any testing suite related with sdk, you are required to build a sample app. 

below is the requirement for sample app.

* Web application/Docker image: *The data emitter needs to be a web application serving a http endpoint, and needs to be able to built as a Docker image and run for testing as a Docker app. A Dockerfile needs to be provided.

* Response pattern: The response of each URL path need to be followed using the data structure listed below i.e. you just need to return the traceid. [json/application]
```json
{
"traceId": "xxxx"
}
```

* Unique ID. [skip it if you just do trace not metric]Every time the validator, which is a component in the testing framework, calls the URL, the validator will generate a test-suite-id, which is unique for each run. The URL parameter, for example: http://x.x.x.x:8080/get-trace?testing-id=12313123241, can be used as an unique identifier for the metric e.g. adding this id as part of the metric name so that the metric could be different for each testing run. Note that no matter which http method you use, it will not be in the body, but just in the url parameter.
Environment Variable: There will be some env vars which will be set while running this web application. The env vars will be but not limited to:
    1. OTEL_EXPORTER_OTLP_ENDPOINT
    2. OTEL_RESOURCE_ATTRIBUTES
    3. LISTEN_ADDRESS : *This is a mandatory environment variable. This web application address, for example (0.0.0.0:8080), makes the address controllable by the upper level; prevents port conflict with any other software on the testbed and also helps us limit the web application access within a private subnet.
    
* Support multiple URL path: *There’s no limitation on the URL path. There could be multiple URLs in this web application depending on the number of test cases you want.

* Keep “/” accessible with response code 200.: This “/” will be used for the load balancer health check.
