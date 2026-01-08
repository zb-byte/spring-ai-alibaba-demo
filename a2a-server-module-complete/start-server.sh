#!/bin/bash

# A2A Complete Server Startup Script

echo "========================================"
echo "Starting A2A Complete Server"
echo "Supporting: REST, gRPC, and JSON-RPC"
echo "========================================"

# Change to the module directory
cd "$(dirname "$0")"

# Build the project with Maven
echo "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed! Exiting."
    exit 1
fi

echo "Build successful!"
echo ""
echo "Starting server..."
echo "REST will be available at: http://localhost:7003"
echo "gRPC will be available at: http://localhost:9092"
echo "JSON-RPC will be available at: http://localhost:7003/a2a"
echo ""

# Run the application
mvn spring-boot:run
