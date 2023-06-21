FROM gradle:8.1.1-jdk17 as build

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src

RUN gradle build
RUN tar -xvf build/distributions/app.tar

FROM amazoncorretto:17

WORKDIR /app
COPY --from=build /app/app .

ENTRYPOINT ["/app/bin/app"]