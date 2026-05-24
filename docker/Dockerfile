# syntax=docker/dockerfile:1.7

################################
# Build stage
################################
FROM --platform=$TARGETPLATFORM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Maven settings.xml authenticating the github-yafva repo (id matches pom.xml).
# GITHUB_ACTOR / GITHUB_TOKEN are read from BuildKit secrets at build time so
# the token never lands in an image layer. Lives outside /root/.m2 because
# the BuildKit cache mount below shadows everything under that path.
COPY docker/settings.xml /etc/maven/settings.xml

COPY pom.xml ./
COPY .mvn/ .mvn/

RUN --mount=type=cache,id=m2cache,target=/root/.m2 \
  --mount=type=secret,id=gh_actor \
  --mount=type=secret,id=gh_token \
  GITHUB_ACTOR="$(cat /run/secrets/gh_actor)" \
  GITHUB_TOKEN="$(cat /run/secrets/gh_token)" \
  mvn -s /etc/maven/settings.xml -B -q -ntp -DskipTests \
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
  -Dorg.slf4j.simpleLogger.log.org.eclipse.aether.transfer=warn \
  dependency:go-offline

COPY src/ src/

RUN --mount=type=cache,id=m2cache,target=/root/.m2 \
  --mount=type=secret,id=gh_actor \
  --mount=type=secret,id=gh_token \
  GITHUB_ACTOR="$(cat /run/secrets/gh_actor)" \
  GITHUB_TOKEN="$(cat /run/secrets/gh_token)" \
  mvn -s /etc/maven/settings.xml -T 1C -B -q -ntp -DskipTests clean package

################################
# Runtime stage
################################
FROM eclipse-temurin:21-jre as production
WORKDIR /app

# Optional params
ARG APP_VERSION=dev
ARG RUNTIME_UID=10001
ARG RUNTIME_GID=10001

# ENV tracing
LABEL org.opencontainers.image.version=$APP_VERSION
ENV APP_VERSION=$APP_VERSION \
    JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/./urandom -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

# Minimal curl for HEALTHCHECK
RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create the user UID/GID (if there is one - ignore the error)
RUN set -eux; \
    getent group "${RUNTIME_GID}" || groupadd -r -g "${RUNTIME_GID}" app; \
    getent passwd "${RUNTIME_UID}" || useradd -r -u "${RUNTIME_UID}" -g "${RUNTIME_GID}" -s /sbin/nologin app

COPY --from=builder --chown=${RUNTIME_UID}:${RUNTIME_GID} /app/target/yafva*.jar /app/yafva.jar

RUN mkdir -p /home/app/.fhir/packages && chown -R ${RUNTIME_UID}:${RUNTIME_GID} /home/app

USER ${RUNTIME_UID}:${RUNTIME_GID}
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/yafva.jar"]