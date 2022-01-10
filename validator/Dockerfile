FROM gradle:7.3.3-jdk11 as build

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src

RUN gradle build
RUN tar -xvf build/distributions/app.tar

FROM amazoncorretto:11

WORKDIR /app
COPY --from=build /app/app .

ENTRYPOINT ["/app/bin/app"]