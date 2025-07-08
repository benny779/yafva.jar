#!/bin/bash

echo "Building YAFVA.JAR Docker image..."

# Get version from pom.xml
VERSION=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

echo "Building version: $VERSION"

# Build the Docker image
docker build -f docker/Dockerfile -t yafva-jar:$VERSION .
docker tag yafva-jar:$VERSION yafva-jar:latest

echo "Docker image built successfully!"
echo
echo "Available images:"
docker images yafva-jar

echo
echo "To run the container:"
echo "docker run -p 8080:8080 yafva-jar:latest"
echo
echo "To deploy to Kubernetes:"
echo "kubectl apply -f docker/k8s-deployment.yaml"
