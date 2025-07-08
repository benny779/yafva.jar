@echo off
REM Build script for YAFVA.JAR Docker image

echo Building YAFVA.JAR Docker image...

REM Get version from pom.xml
for /f "tokens=2 delims=<>" %%i in ('findstr "<version>" pom.xml') do (
    set VERSION=%%i
    goto :found_version
)
:found_version

echo Building version: %VERSION%

REM Build the Docker image
docker build -f docker/Dockerfile -t yafva-jar:%VERSION% .
docker tag yafva-jar:%VERSION% yafva-jar:latest

echo Docker image built successfully!
echo.
echo Available images:
docker images yafva-jar

echo.
echo To run the container:
echo docker run -p 8080:8080 yafva-jar:latest
echo.
echo To deploy to Kubernetes:
echo kubectl apply -f docker/k8s-deployment.yaml
