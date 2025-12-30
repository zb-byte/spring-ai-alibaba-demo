#!/bin/bash

# 启动 Reviewer Service (A2A Server)
# 端口: 8081

cd "$(dirname "$0")/../reviewer-service"

# 检测并使用 mvnd 或 mvn
if command -v mvnd &> /dev/null; then
    MVN_CMD="mvnd"
    echo "使用 mvnd (Maven Daemon)"
else
    MVN_CMD="mvn"
    echo "使用 mvn (标准 Maven)"
fi

echo "=========================================="
echo "启动 Reviewer Service (A2A Server)"
echo "端口: 8081"
echo "=========================================="
echo ""

$MVN_CMD spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

