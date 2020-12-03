FROM gradle:6.5.0-jdk11 as builder

# build
WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src

RUN gradle build
RUN tar -xvf build/distributions/app.tar

FROM amazoncorretto:11
WORKDIR /app
COPY --from=builder /app/app .

ENV OTEL_RESOURCE_ATTRIBUTES 'service.namespace=AWSOTel,service.name=AWSOTelLoadGenerator'

EXPOSE 4567

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


