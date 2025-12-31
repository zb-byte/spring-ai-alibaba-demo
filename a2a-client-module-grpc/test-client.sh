#!/bin/bash

echo "=== A2A gRPC Client Demo 测试脚本 ==="
echo ""

# 检查客户端是否运行
echo "🔍 检查客户端状态..."
if curl -s http://localhost:7001/api/test-connection > /dev/null; then
    echo "✅ 客户端正在运行"
else
    echo "❌ 客户端未运行，请先启动: ./start-client.sh"
    exit 1
fi
echo ""

# 测试连接
echo "🔗 测试服务器连接..."
curl -s http://localhost:7001/api/test-connection | jq '.' || echo "连接测试完成"
echo ""

# 测试 Agent Card 获取 (HTTP)
echo "📄 测试 Agent Card 获取 (HTTP)..."
curl -s http://localhost:7001/api/agent-card | jq '.' || echo "Agent Card 获取完成"
echo ""

# 测试 Agent Card 获取 (A2A SDK)
echo "🚀 测试 Agent Card 获取 (A2A SDK)..."
curl -s http://localhost:7001/api/agent-card-a2a | jq '.' || echo "A2A Agent Card 获取完成"
echo ""

# 测试消息发送 (HTTP)
echo "📤 测试消息发送 (HTTP)..."
curl -s -X POST http://localhost:7001/api/send-message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello from test script!"}' | jq '.' || echo "HTTP 消息发送完成"
echo ""

# 测试消息发送 (A2A SDK)
echo "🚀 测试消息发送 (A2A SDK)..."
curl -s "http://localhost:7001/api/send-message-a2a?message=Hello%20from%20A2A%20SDK!" | jq '.' || echo "A2A 消息发送完成"
echo ""

# 测试 gRPC 连接
echo "🔌 测试 gRPC 连接..."
curl -s http://localhost:7001/api/test-grpc | jq '.' || echo "gRPC 连接测试完成"
echo ""

echo "✅ 所有测试完成！"
echo ""
echo "📱 Web 界面: http://localhost:7001/"
echo "🔗 API 文档: 见 README.md"