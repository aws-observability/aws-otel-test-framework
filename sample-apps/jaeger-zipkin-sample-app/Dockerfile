FROM gradle as build

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./build/ ./build/

RUN gradle build
RUN tar -xvf build/distributions/jaeger-zipkin-sample-app-1.0.tar

FROM amazoncorretto:11

WORKDIR /app
COPY --from=build /app/jaeger-zipkin-sample-app-1.0 .

ENV HOME=/root

CMD ["/app/bin/jaeger-zipkin-sample-app"]






