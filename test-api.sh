#!/bin/bash

# A2A 传输协议测试脚本
# 使用方法: ./test-api.sh

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Spring AI Alibaba A2A 协议测试"
echo "=========================================="
echo ""

echo "1. 查看当前传输协议类型"
echo "----------------------------------------"
curl -s "${BASE_URL}/demo/transport/type" | jq .
echo ""
echo ""

echo "2. 测试本地 Agent 调用（不通过 A2A）"
echo "----------------------------------------"
curl -s -X POST "${BASE_URL}/demo/transport/local" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}' | jq .
echo ""
echo ""

echo "3. 测试 A2A 协议调用（使用当前配置的协议）"
echo "----------------------------------------"
curl -s -X POST "${BASE_URL}/demo/transport/a2a" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}' | jq .
echo ""
echo ""

echo "4. 获取 Agent Card 信息"
echo "----------------------------------------"
curl -s "${BASE_URL}/demo/transport/agent-card" | jq .
echo ""
echo ""

echo "5. 获取 A2A 标准 Agent Card（/.well-known/agent.json）"
echo "----------------------------------------"
curl -s "${BASE_URL}/.well-known/agent.json" | jq .
echo ""
echo ""

echo "6. 测试流式响应（Server-Sent Events）"
echo "----------------------------------------"
echo "注意：流式响应会持续输出，按 Ctrl+C 停止"
echo ""
curl -N -H "Accept: text/event-stream" \
  -X POST "${BASE_URL}/demo/streaming/a2a" \
  -H "Content-Type: application/json" \
  -d '{"message": "请用一句话介绍 Spring AI Alibaba"}' 2>/dev/null | head -20
echo ""
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="

