FROM openjdk:11-jdk-slim

LABEL com.mindtro.cloud="engin mahmut"

EXPOSE 9008

WORKDIR /opt

# Add & Run Microservice
ADD boiler-ms-cms-api.jar /opt/
ADD application.properties /opt/
ADD banner.txt /opt/
ADD jpa.properties /opt/
ADD bootstrap.properties /opt/
ENTRYPOINT ["java", "-Xmx768m", "-Xms384m", "-jar", "-Dspring.config.location=/opt/", "boiler-ms-cms-api.jar"]
