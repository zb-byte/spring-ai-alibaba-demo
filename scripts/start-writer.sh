#!/bin/bash

# 启动 Writer Service (A2A Client)
# 端口: 8080

cd "$(dirname "$0")/../writer-service"

# 检测并使用 mvnd 或 mvn
if command -v mvnd &> /dev/null; then
    MVN_CMD="mvnd"
    echo "使用 mvnd (Maven Daemon)"
else
    MVN_CMD="mvn"
    echo "使用 mvn (标准 Maven)"
fi

echo "=========================================="
echo "启动 Writer Service (A2A Client)"
echo "端口: 8080"
echo "=========================================="
echo ""

$MVN_CMD spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

