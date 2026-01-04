#!/bin/bash

# A2A REST Client 测试脚本
# 测试 Client 与 Server 的 A2A REST 协议通信

echo "=========================================="
echo "A2A REST Client 测试"
echo "=========================================="

CLIENT_URL="http://localhost:7001"
SERVER_URL="http://localhost:7002"

# 检查 jq 是否安装
if command -v jq &> /dev/null; then
    JQ_CMD="jq ."
else
    JQ_CMD="cat"
    echo "Tip: Install 'jq' for better JSON formatting"
fi

echo ""
echo "1. 检查 Server 连接..."
echo "   GET $SERVER_URL/.well-known/agent-card.json"
echo "---"
if curl -s "$SERVER_URL/.well-known/agent-card.json" > /dev/null 2>&1; then
    echo "✅ Server is running"
else
    echo "❌ Server is not responding. Please start the server first."
    exit 1
fi
echo ""

echo ""
echo "2. 通过 Client 获取 Agent Card..."
echo "   GET $CLIENT_URL/test/agent-card"
echo "---"
curl -s "$CLIENT_URL/test/agent-card" | $JQ_CMD
echo ""

echo ""
echo "3. 通过 Client 发送同步消息..."
echo "   POST $CLIENT_URL/test/send?msg=Hello"
echo "---"
curl -s -X POST "$CLIENT_URL/test/send?msg=Hello%20World" | $JQ_CMD
echo ""

echo ""
echo "4. 通过 Client 发送流式消息..."
echo "   POST $CLIENT_URL/test/stream?msg=Hello"
echo "---"
curl -s -X POST "$CLIENT_URL/test/stream?msg=Hello%20Stream"
echo ""

echo ""
echo "5. 获取 Task 状态..."
echo "   GET $CLIENT_URL/test/task/test-task-001"
echo "---"
curl -s "$CLIENT_URL/test/task/test-task-001" | $JQ_CMD
echo ""

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "A2A REST 协议通信流程:"
echo "  Client (7001) --[HTTP REST]--> Server (7002)"
echo ""
echo "协议端点:"
echo "  - /.well-known/agent-card.json  获取 Agent 能力描述"
echo "  - /v1/message:send              同步发送消息"
echo "  - /v1/message:stream            流式发送消息 (SSE)"
echo "  - /v1/tasks/{taskId}            获取任务状态"
echo "  - /v1/tasks/{taskId}:cancel     取消任务"
