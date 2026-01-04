#!/bin/bash

# A2A REST Client 启动脚本
# 基于 A2A Java SDK v0.3.3.Final

echo "=========================================="
echo "A2A REST Client"
echo "=========================================="

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

echo ""
echo "Configuration:"
echo "  - Client HTTP Port: 7001"
echo "  - Target Server: http://localhost:7002"
echo ""

# 检查 Server 是否运行
echo "Checking if A2A REST Server is running..."
if curl -s "http://localhost:7002/.well-known/agent-card.json" > /dev/null 2>&1; then
    echo "✅ A2A REST Server is running"
else
    echo "⚠️  Warning: A2A REST Server (port 7002) is not responding"
    echo "   Please start the server first: cd ../a2a-server-module-rest && ./start-server.sh"
fi
echo ""

# 编译项目
echo "Building project..."
$MVN_CMD clean compile -q

if [ $? -ne 0 ]; then
    echo "Error: Failed to build project"
    exit 1
fi

# 启动客户端
echo "Starting A2A REST Client on port 7001..."
echo ""
echo "Test Endpoints:"
echo "  - Agent Card: http://localhost:7001/test/agent-card"
echo "  - Send Message: POST http://localhost:7001/test/send?msg=Hello"
echo "  - Stream Message: POST http://localhost:7001/test/stream?msg=Hello"
echo ""
$MVN_CMD spring-boot:run

echo "Client stopped."
