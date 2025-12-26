#!/bin/bash

# 启动 Writer Service (A2A Client)
# 端口: 8080

cd "$(dirname "$0")/../writer-service"

echo "=========================================="
echo "启动 Writer Service (A2A Client)"
echo "端口: 8080"
echo "=========================================="
echo ""

mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

