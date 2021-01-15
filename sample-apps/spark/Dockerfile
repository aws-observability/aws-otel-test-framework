FROM gradle as build

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./settings.gradle ./settings.gradle

RUN gradle build
RUN tar -xvf build/distributions/spark-sample-app-1.0.tar
RUN wget https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/v0.12.1-aws.1/aws-opentelemetry-agent.jar

FROM amazoncorretto:11 

WORKDIR /app
COPY --from=build /app/spark-sample-app-1.0 .
COPY --from=build /app/aws-opentelemetry-agent.jar ./aws-opentelemetry-agent.jar

ENV JAVA_OPTS "-javaagent:/app/aws-opentelemetry-agent.jar -Dotel.imr.export.interval=1000"
ENV HOME=/root

CMD ["/app/bin/spark-sample-app"]






