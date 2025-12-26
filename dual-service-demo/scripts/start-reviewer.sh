#!/bin/bash

# 启动 Reviewer Service (A2A Server)
# 端口: 8081

cd "$(dirname "$0")/../reviewer-service"

echo "=========================================="
echo "启动 Reviewer Service (A2A Server)"
echo "端口: 8081"
echo "=========================================="
echo ""

mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

