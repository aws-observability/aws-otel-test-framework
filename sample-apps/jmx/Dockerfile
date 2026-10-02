# Build the war file
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .
RUN mvn package

# Tomcat w/ war (stay on 9.0 line: app targets javax.servlet / Servlet 4.0)
FROM tomcat:9.0-jre21-temurin-noble
ARG AGENT_VER=0.12.0

# Pick up OS package updates published after the base image was built, and remove the
# tools the sample app does not use at runtime (the exporter jar below is fetched with
# ADD, so neither curl nor wget is needed in the image).
RUN export DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get upgrade -y \
    && apt-get purge -y --auto-remove curl wget gnupg \
    && rm -rf /var/lib/apt/lists/*

# Download JMX exporter agent
RUN mkdir -p /opt/jmx_exporter
# visit https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/ to find other versions
ADD --chmod=0644 https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${AGENT_VER}/jmx_prometheus_javaagent-${AGENT_VER}.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar
COPY config.yaml /opt/jmx_exporter
COPY setenv.sh /usr/local/tomcat/bin
RUN chmod  o+x /usr/local/tomcat/bin/setenv.sh
RUN ls /opt/jmx_exporter

# Copy war
# http://localhost:8888/samplejmx/hello-servlet
COPY --from=build /app/target/samplejmx-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/samplejmx.war

ENTRYPOINT ["catalina.sh", "run"]