# 双服务 A2A 演示 - 快速开始

## 架构说明

```
┌─────────────────────┐         A2A Protocol        ┌─────────────────────┐
│   Writer Service    │ ──────────────────────────> │  Reviewer Service   │
│   (Port 8080)       │    JSON-RPC 2.0            │   (Port 8081)       │
│                     │                             │                     │
│  - Writer Agent     │                             │  - Reviewer Agent   │
│  - A2A Client       │                             │  - A2A Server       │
└─────────────────────┘                             └─────────────────────┘
```

## 快速启动

### 方式一：使用脚本（推荐）

**终端 1 - 启动 Reviewer Service：**
```bash
cd dual-service-demo
./scripts/start-reviewer.sh
```

**终端 2 - 启动 Writer Service：**
```bash
cd dual-service-demo
./scripts/start-writer.sh
```

**终端 3 - 运行测试：**
```bash
cd dual-service-demo
./scripts/test-demo.sh
```

### 方式二：手动启动

**1. 启动 Reviewer Service：**
```bash
cd dual-service-demo/reviewer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**2. 启动 Writer Service：**
```bash
cd dual-service-demo/writer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

**3. 测试：**
```bash
curl -X POST http://localhost:8080/api/write-and-review \
  -H "Content-Type: application/json" \
  -d '{"topic": "Spring AI Alibaba 的优势"}'
```

## 演示流程

### 1. 查看 Reviewer Agent Card

```bash
curl http://localhost:8081/.well-known/agent.json | jq .
```

**输出示例：**
```json
{
  "name": "reviewer-agent",
  "description": "一个专业的文章评审 Agent",
  "url": "http://127.0.0.1:8081/a2a",
  "preferred_transport": "JSONRPC"
}
```

### 2. Writer Service 生成文章

Writer Agent 根据主题生成文章。

### 3. 通过 A2A 协议调用 Reviewer Service

Writer Service 通过 A2A 协议（JSON-RPC 2.0）将文章发送给 Reviewer Service。

**协议细节：**
- 协议：JSON-RPC 2.0
- 端点：`http://127.0.0.1:8081/a2a`
- 方法：`a2a.message.send`

### 4. Reviewer Service 评审文章

Reviewer Agent 对文章进行评审和修改。

### 5. 返回最终结果

Writer Service 返回评审后的文章。

## 测试端点

### Writer Service (8080)

- `GET /api/health` - 健康检查
- `POST /api/write-and-review` - 写文章并评审

### Reviewer Service (8081)

- `GET /.well-known/agent.json` - Agent Card
- `POST /a2a` - A2A 协议端点

## 完整测试示例

```bash
# 1. 检查 Reviewer Service
curl http://localhost:8081/.well-known/agent.json

# 2. 检查 Writer Service
curl http://localhost:8080/api/health

# 3. 完整流程演示
curl -X POST http://localhost:8080/api/write-and-review \
  -H "Content-Type: application/json" \
  -d '{"topic": "Spring AI Alibaba 的优势"}' | jq .
```

## 预期响应

```json
{
  "topic": "Spring AI Alibaba 的优势",
  "originalArticle": "Spring AI Alibaba 是一个...",
  "reviewedArticle": "Spring AI Alibaba 是一个优秀的框架...",
  "protocol": "A2A (JSON-RPC 2.0)",
  "flow": {
    "step1": "Writer Agent 生成文章",
    "step2": "通过 A2A 协议调用 Reviewer Service",
    "step3": "Reviewer Agent 评审并修改文章",
    "step4": "返回最终结果"
  }
}
```

## 演示要点

### ✅ 展示的能力

1. **真正的分布式通信**
   - 两个独立的 Spring Boot 服务
   - 通过网络协议通信
   - 跨进程/跨服务调用

2. **A2A 协议标准化**
   - 使用标准的 A2A 协议格式
   - Agent Card 发现机制
   - JSON-RPC 2.0 协议

3. **Agent 协作**
   - Writer Agent → Reviewer Agent
   - 展示 Agent 之间的任务传递
   - 展示多 Agent 协作场景

4. **协议无关性**
   - 可以切换不同的传输协议
   - 同一套 Agent 代码，支持多种协议

## 故障排查

### Reviewer Service 无法启动

1. 检查端口 8081 是否被占用
2. 检查 LLM API Key 配置
3. 查看日志：`reviewer-service/logs/`

### Writer Service 无法连接 Reviewer Service

1. 确认 Reviewer Service 已启动
2. 检查 `application.yml` 中的 `reviewer-agent-url` 配置
3. 测试连接：`curl http://localhost:8081/.well-known/agent.json`

### 响应为空

1. 检查 LLM API Key 是否正确
2. 检查网络连接
3. 查看两个服务的日志

## 下一步

- 尝试切换不同的传输协议（gRPC、REST）
- 添加更多 Agent 到协作链
- 集成 Nacos 服务发现

