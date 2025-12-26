# 双服务 A2A 演示 - 快速开始

## 🎯 演示目标

展示两个独立的 Spring Boot 服务通过 A2A 协议进行分布式 Agent 通信。

## 📋 前置要求

1. JDK 17+
2. Maven 3.6+
3. LLM API Key（配置在环境变量或 application.yml 中）

## 🚀 快速启动（3 个步骤）

### 步骤 1: 启动 Reviewer Service（必须先启动）

**终端 1：**
```bash
cd dual-service-demo
./scripts/start-reviewer.sh
```

等待看到：
```
Started ReviewerApplication in X.XXX seconds
```

**验证：**
```bash
curl http://localhost:8081/.well-known/agent.json
```

### 步骤 2: 启动 Writer Service

**终端 2：**
```bash
cd dual-service-demo
./scripts/start-writer.sh
```

等待看到：
```
Started WriterApplication in X.XXX seconds
```

**验证：**
```bash
curl http://localhost:8080/api/health
```

### 步骤 3: 运行演示

**终端 3：**
```bash
cd dual-service-demo
./scripts/test-demo.sh
```

## 📊 演示流程

```
用户请求: {"topic": "Spring AI Alibaba 的优势"}
   ↓
┌─────────────────────────────────┐
│  Writer Service (Port 8080)     │
│  Writer Agent 生成文章          │
└─────────────────────────────────┘
   ↓ A2A Protocol (JSON-RPC 2.0)
   ↓ HTTP POST http://127.0.0.1:8081/a2a
┌─────────────────────────────────┐
│  Reviewer Service (Port 8081)   │
│  Reviewer Agent 评审文章        │
└─────────────────────────────────┘
   ↓
返回评审后的文章
```

## 🧪 手动测试

### 1. 查看 Reviewer Agent Card

```bash
curl http://localhost:8081/.well-known/agent.json | jq .
```

### 2. 测试完整流程

```bash
curl -X POST http://localhost:8080/api/write-and-review \
  -H "Content-Type: application/json" \
  -d '{"topic": "Spring AI Alibaba 的优势"}' | jq .
```

### 3. 查看 A2A 协议原始请求

```bash
curl -X POST http://localhost:8081/a2a \
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
  }' | jq .
```

## 📁 项目结构

```
dual-service-demo/
├── README.md                    # 详细说明
├── QUICK_START.md              # 快速开始（本文件）
├── DEMO_GUIDE.md               # 演示指南
├── reviewer-service/           # Reviewer Service (A2A Server)
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/reviewer/
│           │       ├── ReviewerApplication.java
│           │       └── config/
│           │           ├── LlmConfiguration.java
│           │           └── LlmProperties.java
│           └── resources/
│               └── application.yml
├── writer-service/             # Writer Service (A2A Client)
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/writer/
│           │       ├── WriterApplication.java
│           │       ├── config/
│           │       │   ├── LlmConfiguration.java
│           │       │   └── LlmProperties.java
│           │       └── web/
│           │           └── WriteAndReviewController.java
│           └── resources/
│               └── application.yml
└── scripts/
    ├── start-reviewer.sh      # 启动 Reviewer Service
    ├── start-writer.sh         # 启动 Writer Service
    └── test-demo.sh            # 测试脚本
```

## ⚠️ 常见问题

### 问题 1: Reviewer Service 启动失败

**检查：**
- 端口 8081 是否被占用
- LLM API Key 是否正确配置

**解决：**
```bash
# 检查端口
lsof -i :8081

# 修改端口（如果需要）
# 编辑 reviewer-service/src/main/resources/application.yml
# 修改 server.port
```

### 问题 2: Writer Service 无法连接 Reviewer Service

**检查：**
- Reviewer Service 是否已启动
- `reviewer.agent.url` 配置是否正确

**测试连接：**
```bash
curl http://localhost:8081/.well-known/agent.json
```

### 问题 3: 响应为空

**检查：**
- 两个服务的日志是否有错误
- LLM API Key 是否正确
- 网络连接是否正常

**查看日志：**
- Reviewer Service: 查看启动终端的日志
- Writer Service: 查看启动终端的日志

## 🎤 给领导演示时的说明

### 1. 架构说明

"这是一个双服务的 A2A 演示，展示了两个完全独立的 Spring Boot 服务如何通过 A2A 协议进行分布式通信。"

### 2. 流程说明

"当用户请求写文章时：
1. Writer Service 的 Writer Agent 先生成文章
2. 然后通过 A2A 协议（JSON-RPC 2.0）调用 Reviewer Service
3. Reviewer Service 的 Reviewer Agent 对文章进行评审和修改
4. 返回评审后的文章给用户"

### 3. 协议说明

"这里使用的是 JSON-RPC 2.0 协议，但 A2A 还支持 gRPC 和 HTTP+JSON/REST，可以根据场景选择最适合的协议。"

### 4. 分布式说明

"两个服务是完全独立的，可以部署在不同的机器上，通过网络协议通信，这就是真正的分布式 Agent 通信。"

## 📚 更多文档

- `README.md` - 详细架构说明
- `DEMO_GUIDE.md` - 完整的演示指南
- `QUICK_START.md` - 快速开始指南

