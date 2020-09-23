FROM gradle:6.5.0-jdk11 as builder

# build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build
RUN wget https://github.com/anuraaga/aws-opentelemetry-java-instrumentation/releases/download/v0.9.0-alpha.1/aws-opentelemetry-agent.jar
RUN tar -xvf build/distributions/TraceEmitter-1.0.tar

FROM amazoncorretto:8
WORKDIR /app
COPY --from=builder /home/gradle/src/TraceEmitter-1.0 /app/
COPY --from=builder /home/gradle/src/aws-opentelemetry-agent.jar /app/
RUN ls -R

ENV OTEL_OTLP_ENDPOINT localhost:55680
ENV OTEL_RESOURCE_ATTRIBUTES 'service.name=OTIntegTest'
ENV JAVA_OPTS "-javaagent:/app/aws-opentelemetry-agent.jar -Dotel.imr.export.interval=1000"

EXPOSE 4567

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


