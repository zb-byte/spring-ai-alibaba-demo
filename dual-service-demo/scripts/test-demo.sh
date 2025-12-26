#!/bin/bash

# 双服务 A2A 演示测试脚本

REVIEWER_URL="http://localhost:8081"
WRITER_URL="http://localhost:8080"

echo "=========================================="
echo "双服务 A2A 协议演示测试"
echo "=========================================="
echo ""

echo "【步骤 1】检查 Reviewer Service (A2A Server)"
echo "----------------------------------------"
if curl -s "${REVIEWER_URL}/.well-known/agent.json" > /dev/null 2>&1; then
    echo "✅ Reviewer Service 运行正常"
    echo ""
    echo "Agent Card 信息："
    curl -s "${REVIEWER_URL}/.well-known/agent.json" | jq -r '
        "  - 名称: " + .name,
        "  - 描述: " + .description,
        "  - URL: " + .url,
        "  - 协议: " + .preferred_transport
    '
else
    echo "❌ Reviewer Service 未启动，请先启动 Reviewer Service"
    echo "   运行: ./scripts/start-reviewer.sh"
    exit 1
fi
echo ""
echo ""

echo "【步骤 2】检查 Writer Service (A2A Client)"
echo "----------------------------------------"
if curl -s "${WRITER_URL}/api/health" > /dev/null 2>&1; then
    echo "✅ Writer Service 运行正常"
    curl -s "${WRITER_URL}/api/health" | jq .
else
    echo "❌ Writer Service 未启动，请先启动 Writer Service"
    echo "   运行: ./scripts/start-writer.sh"
    exit 1
fi
echo ""
echo ""

echo "【步骤 3】演示完整流程：写文章 → A2A 调用 → 评审文章"
echo "----------------------------------------"
echo "请求：Writer Service 生成文章，然后通过 A2A 协议调用 Reviewer Service 进行评审"
echo ""
RESPONSE=$(curl -s -X POST "${WRITER_URL}/api/write-and-review" \
  -H "Content-Type: application/json" \
  -d '{"topic": "Spring AI Alibaba 的优势"}')

if [ $? -eq 0 ]; then
    echo "✅ 请求成功"
    echo ""
    echo "$RESPONSE" | jq -r '
        "主题: " + .topic,
        "",
        "原始文章:",
        .originalArticle,
        "",
        "评审后文章:",
        .reviewedArticle,
        "",
        "使用的协议: " + .protocol
    '
    echo ""
    echo "流程说明："
    echo "$RESPONSE" | jq -r '.flow | to_entries[] | "  \(.key): \(.value)"'
else
    echo "❌ 请求失败"
    exit 1
fi
echo ""
echo ""

echo "【步骤 4】查看 A2A 协议细节"
echo "----------------------------------------"
echo "直接调用 Reviewer Service 的 A2A 端点："
echo ""
curl -s -X POST "${REVIEWER_URL}/a2a" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "a2a.message.send",
    "params": {
      "message": {
        "role": "user",
        "content": "请评审这段文字：Spring AI Alibaba 是一个优秀的框架。"
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
echo "演示完成！"
echo "=========================================="
echo ""
echo "总结："
echo "✅ 展示了两个独立服务之间的 A2A 协议通信"
echo "✅ Writer Service 通过 A2A 协议调用 Reviewer Service"
echo "✅ 展示了完整的 Agent 协作流程"
echo "✅ 展示了 JSON-RPC 2.0 协议的使用"
echo ""

