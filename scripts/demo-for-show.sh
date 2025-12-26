#!/bin/bash

# A2A 协议演示脚本 - 给领导演示用
# 按顺序展示 A2A 协议的核心能力

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Spring AI Alibaba A2A 协议演示"
echo "=========================================="
echo ""

echo "【演示 1】Agent Card 发现机制"
echo "----------------------------------------"
echo "A2A 协议的核心：每个 Agent 都通过 Agent Card 声明自己的能力"
echo ""
curl -s "${BASE_URL}/.well-known/agent.json" | jq -r '
  "Agent 名称: " + .name,
  "Agent 描述: " + .description,
  "Agent URL: " + .url,
  "支持的传输协议: " + (.additional_interfaces[]?.transport // .preferred_transport),
  "支持流式通信: " + (.capabilities.streaming // false | tostring)
'
echo ""
echo ""

echo "【演示 2】当前使用的传输协议"
echo "----------------------------------------"
echo "A2A 支持三种传输协议：JSON-RPC 2.0、gRPC、HTTP+JSON/REST"
echo ""
curl -s "${BASE_URL}/demo/transport/type" | jq -r '
  "当前协议: " + .currentTransportType,
  "支持的协议: " + (.supportedTypes | join(", "))
'
echo ""
echo ""

echo "【演示 3】A2A 协议调用（非流式）"
echo "----------------------------------------"
echo "通过 A2A 协议调用远程 Agent，获取完整响应"
echo ""
curl -s -X POST "${BASE_URL}/demo/transport/a2a" \
  -H "Content-Type: application/json" \
  -d '{"message": "请用一句话介绍 Spring AI Alibaba"}' | jq -r '
  "输入: " + .input,
  "协议: " + .protocol,
  "响应: " + .response
'
echo ""
echo ""

echo "【演示 4】A2A 协议调用（流式）"
echo "----------------------------------------"
echo "展示流式通信能力，实时返回数据流"
echo "注意：流式响应会持续输出，这里只显示前 5 条"
echo ""
curl -N -H "Accept: text/event-stream" \
  -X POST "${BASE_URL}/demo/streaming/a2a" \
  -H "Content-Type: application/json" \
  -d '{"message": "请用一句话介绍 A2A 协议"}' 2>/dev/null | head -5
echo ""
echo ""

echo "【演示 5】JSON-RPC 2.0 原始协议"
echo "----------------------------------------"
echo "展示底层 JSON-RPC 2.0 协议格式"
echo ""
curl -s -X POST "${BASE_URL}/a2a" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "a2a.message.send",
    "params": {
      "message": {
        "role": "user",
        "content": "你好"
      }
    },
    "id": 1
  }' | jq -r '
  "协议版本: " + .jsonrpc,
  "方法: " + (.method // "N/A"),
  "响应ID: " + (.id | tostring)
'
echo ""
echo ""

echo "=========================================="
echo "演示总结"
echo "=========================================="
echo "✅ Agent Card 发现机制"
echo "✅ 三种传输协议支持（JSON-RPC/gRPC/REST）"
echo "✅ 流式和非流式通信"
echo "✅ 标准化协议格式"
echo ""
echo "当前演示展示了 A2A 协议的核心能力。"
echo "在实际生产环境中，Agent 可以部署在不同的服务中，"
echo "通过网络协议进行分布式通信和协作。"
echo "=========================================="

