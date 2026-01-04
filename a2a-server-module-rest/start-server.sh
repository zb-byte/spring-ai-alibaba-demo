#!/bin/bash

# A2A REST Server 启动脚本
# 基于 A2A Java SDK v0.3.3.Final + Spring AI

echo "=========================================="
echo "A2A REST Server (Spring AI)"
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

# 检查环境变量
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Warning: OPENAI_API_KEY not set, using default value"
fi

echo ""
echo "Configuration:"
echo "  - HTTP Port: 7002"
echo "  - API Base URL: ${OPENAI_BASE_URL:-https://ark.cn-beijing.volces.com}"
echo "  - Model: ${OPENAI_MODEL:-deepseek-v3-250324}"
echo ""

# 编译项目
echo "Building project..."
$MVN_CMD clean compile -q

if [ $? -ne 0 ]; then
    echo "Error: Failed to build project"
    exit 1
fi

# 启动服务器
echo "Starting A2A REST Server on port 7002..."
echo ""
echo "API Endpoints:"
echo "  - Agent Card: http://localhost:7002/.well-known/agent-card.json"
echo "  - Send Message: POST http://localhost:7002/v1/message:send"
echo "  - Stream Message: POST http://localhost:7002/v1/message:stream"
echo ""
$MVN_CMD spring-boot:run

echo "Server stopped."
