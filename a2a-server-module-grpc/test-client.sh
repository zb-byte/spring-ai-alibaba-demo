#!/bin/bash

# A2A gRPC 客户端测试脚本

HOST=${1:-localhost}
PORT=${2:-9090}
MESSAGE=${3:-"Hello, A2A Echo Agent!"}

echo "Testing A2A gRPC Server..."
echo "Host: $HOST"
echo "Port: $PORT"
echo "Message: $MESSAGE"
echo ""

# 检查服务器是否运行
echo "Checking if server is running..."
if ! nc -z $HOST $PORT 2>/dev/null; then
    echo "Error: Cannot connect to $HOST:$PORT"
    echo "Please make sure the A2A gRPC Server is running"
    exit 1
fi

echo "Server is running. Testing Agent Card endpoint..."

# 测试 Agent Card 端点
HTTP_PORT=7002
AGENT_CARD_URL="http://$HOST:$HTTP_PORT/.well-known/agent-card.json"

if command -v curl &> /dev/null; then
    echo "Fetching Agent Card from $AGENT_CARD_URL"
    curl -s "$AGENT_CARD_URL" | python3 -m json.tool 2>/dev/null || curl -s "$AGENT_CARD_URL"
    echo ""
else
    echo "curl not found, skipping Agent Card test"
fi

# 测试 gRPC 调用
echo "Testing gRPC call..."

# 使用 Java 客户端测试
echo "Running Java gRPC client test..."
mvn exec:java -Dexec.mainClass="com.example.a2aserver.util.GrpcClientTester" \
    -Dexec.args="$HOST $PORT \"$MESSAGE\"" -q

echo ""
echo "Test completed."