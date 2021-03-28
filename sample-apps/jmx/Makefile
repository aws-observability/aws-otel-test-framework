IMG=prometheus-sample-tomcat-jmx

build:
	docker build -t $(IMG) .

run:
	@echo sample app is http://localhost:8080/samplejmx/
	@echo prometheus metrics is http://localhost:9404/metrics
	docker run --rm -p 8080:8080 -p 9404:9404 $(IMG)