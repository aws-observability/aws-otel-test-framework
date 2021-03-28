# Build the war file
FROM maven:3-openjdk-8 as build

WORKDIR /app
COPY . .
RUN mvn package

# Tomcat w/ war
FROM tomcat:9.0-jdk8-openjdk
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