FROM gradle:6.5.0-jdk11 as builder

# build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build
RUN tar -xvf build/distributions/load-generator-1.0.tar

FROM amazoncorretto:8
WORKDIR /app
COPY --from=builder /home/gradle/src/load-generator-1.0 /app/
RUN ls -R

ENV OTEL_RESOURCE_ATTRIBUTES 'service.namespace=AWSOTel,service.name=AWSOTelLoadGenerator'

EXPOSE 4567

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


