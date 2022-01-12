FROM gradle:7.3.3-jdk11 as builder

# build
WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./build/ ./build/

# binary version variable  --build-arg <VER>=<1.x>
# default 1.0
ARG VER=1.0

RUN gradle build
RUN tar -xvf build/distributions/load-generator-${VER}.tar

FROM amazoncorretto:11
ARG VER=1.0

WORKDIR /app
COPY --from=builder /app/load-generator-${VER} .

ENV OTEL_RESOURCE_ATTRIBUTES 'service.namespace=AWSOTel,service.name=AWSOTelLoadGenerator'

EXPOSE 4567

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


