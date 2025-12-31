#!/bin/bash

# A2A gRPC Server 启动脚本

echo "Starting A2A Echo Agent gRPC Server..."

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# 检查 Maven 环境 (支持 mvnd 或 mvn)
if command -v mvnd &> /dev/null; then
    MVN_CMD="mvnd"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
else
    echo "Error: Neither mvnd nor mvn is installed or in PATH"
    exit 1
fi

echo "Using Maven command: $MVN_CMD"

# 编译项目
echo "Building project..."
$MVN_CMD clean compile -q

if [ $? -ne 0 ]; then
    echo "Error: Failed to build project"
    exit 1
fi

# 启动服务器
echo "Starting server on gRPC port 9090 and HTTP port 7002..."
$MVN_CMD spring-boot:run

echo "Server stopped."
