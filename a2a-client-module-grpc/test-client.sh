#!/bin/bash

# A2A gRPC Client 测试脚本
# 用于测试客户端与服务器的连接和消息发送

echo "=========================================="
echo "A2A gRPC Client 测试"
echo "=========================================="

CLIENT_URL="http://localhost:7001"

echo ""
echo "1. 测试 HTTP 连接..."
curl -s "$CLIENT_URL/api/test-connection" | jq .

echo ""
echo "2. 测试 gRPC 连接..."
curl -s "$CLIENT_URL/api/test-grpc" | jq .

echo ""
echo "3. 初始化 A2A 客户端..."
curl -s -X POST "$CLIENT_URL/api/init-client" | jq .

echo ""
echo "4. 获取 Agent Card (HTTP)..."
curl -s "$CLIENT_URL/api/agent-card" | jq .

echo ""
echo "5. 获取 Agent Card (gRPC)..."
curl -s "$CLIENT_URL/api/agent-card-a2a" | jq .

echo ""
echo "6. 发送测试消息 (gRPC)..."
curl -s "$CLIENT_URL/api/send-message-a2a?message=Hello%20A2A%20Agent" | jq .

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
