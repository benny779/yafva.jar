# Multi-stage build for optimized production image
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Copy source code
COPY src ./src

# Install Maven and build the application
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/* && \
    mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r yafva && useradd -r -g yafva yafva

# Copy the JAR file from builder stage
COPY --from=builder /app/target/yafva*.jar /app/yafva.jar

# Create FHIR cache directory
RUN mkdir -p /root/.fhir && chown -R yafva:yafva /app /root/.fhir

# Switch to non-root user
USER yafva

EXPOSE 8080

# Health check for Kubernetes readiness/liveness probes
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/yafva.jar"]