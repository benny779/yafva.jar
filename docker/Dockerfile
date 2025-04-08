FROM openjdk:21-jdk-slim

WORKDIR /app

COPY pom.xml /app
COPY src /app/src

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

RUN mvn clean package -DskipTests

COPY target/outburn-fhir-validator-*.jar /app/outburn-fhir-validator.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/outburn-fhir-validator.jar"]