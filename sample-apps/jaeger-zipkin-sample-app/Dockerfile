FROM gradle:8.1.1-jdk17 as builder

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./build/ ./build/

# binary version variable  --build-arg <VER>=<1.x>
# default 1.0
ARG VER=1.0

# assume app is already built since it will not build in docker container
RUN tar -xvf build/distributions/jaeger-zipkin-sample-app-${VER}.tar

FROM amazoncorretto:17

WORKDIR /app
ARG VER=1.0
COPY --from=builder /app/jaeger-zipkin-sample-app-${VER} .

ENV HOME=/root

CMD ["/app/bin/jaeger-zipkin-sample-app"]






