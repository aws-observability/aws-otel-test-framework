# Build the war file
# Build with JDK 17 (source/target stays 1.8 via the pom) on a maintained base image.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .
RUN mvn package

# Tomcat w/ war
# Stay on the Tomcat 9.0 line: the sample app targets javax.servlet (Servlet 4.0);
# Tomcat 10+ moved to the jakarta namespace and would require an app migration.
# Use a current, supported base (Ubuntu 24.04 "noble" + Temurin JRE 21) to clear the
# Debian 10 (EOL) OS CVEs and pick up the latest Tomcat 9.0.x patch release.
FROM tomcat:9.0-jre21-temurin-noble
ARG AGENT_VER=0.12.0

# Download JMX exporter agent
RUN mkdir -p /opt/jmx_exporter
# visit https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/ to find other versions
RUN wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${AGENT_VER}/jmx_prometheus_javaagent-${AGENT_VER}.jar -O /opt/jmx_exporter/jmx_prometheus_javaagent.jar
COPY config.yaml /opt/jmx_exporter
COPY setenv.sh /usr/local/tomcat/bin
RUN chmod  o+x /usr/local/tomcat/bin/setenv.sh
RUN ls /opt/jmx_exporter

# Copy war
# http://localhost:8888/samplejmx/hello-servlet
COPY --from=build /app/target/samplejmx-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/samplejmx.war

ENTRYPOINT ["catalina.sh", "run"]