FROM gradle:6.5.0-jdk11 as builder

# build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :validator:build
RUN curl -O https://releases.hashicorp.com/terraform/0.13.3/terraform_0.13.3_linux_amd64.zip \
  && unzip terraform_0.13.3_linux_amd64.zip -d /usr/bin/

RUN tar -xvf /home/gradle/src/validator/build/distributions/validator.tar


## runner
FROM amazoncorretto:11

COPY --from=builder /home/gradle/src/validator /app/validator
COPY --from=builder /home/gradle/src/terraform /app/terraform
COPY --from=builder /usr/bin/terraform /usr/bin/

WORKDIR /app

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
