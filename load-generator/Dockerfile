FROM gradle as build

# build
WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./build/ ./build/

RUN gradle build
RUN tar -xvf build/distributions/load-generator-1.0.tar

FROM amazoncorretto:11
WORKDIR /app
COPY --from=build /app/load-generator-1.0 .

ENV OTEL_RESOURCE_ATTRIBUTES 'service.namespace=AWSOTel,service.name=AWSOTelLoadGenerator'

EXPOSE 4567

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


