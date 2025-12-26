# 双服务 A2A 演示

本演示展示两个独立的 Spring Boot 服务通过 A2A 协议进行分布式通信。

## 架构说明

```
┌─────────────────────┐         A2A Protocol        ┌─────────────────────┐
│   Writer Service    │ ──────────────────────────> │  Reviewer Service   │
│   (Port 8080)       │    JSON-RPC 2.0 / gRPC /    │   (Port 8081)       │
│                     │         HTTP+JSON           │                     │
│  - Writer Agent     │                             │  - Reviewer Agent   │
│  - A2A Client       │                             │  - A2A Server       │
└─────────────────────┘                             └─────────────────────┘
         │                                                    │
         │                                                    │
         └────────────────────────────────────────────────────┘
                          User Request
```

## 服务说明

### Writer Service (端口 8080)
- **功能**：接收用户请求，生成文章
- **Agent**：Writer Agent（本地 ReactAgent）
- **A2A Client**：通过 A2A 协议调用 Reviewer Service
- **端点**：
  - `POST /api/write-and-review` - 写文章并评审

### Reviewer Service (端口 8081)
- **功能**：接收文章并进行评审
- **Agent**：Reviewer Agent（本地 ReactAgent）
- **A2A Server**：暴露 A2A 协议端点
- **端点**：
  - `GET /.well-known/agent.json` - Agent Card
  - `POST /a2a` - A2A 协议端点

## 快速开始

### 1. 启动 Reviewer Service（服务 B）

```bash
cd reviewer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### 2. 启动 Writer Service（服务 A）

```bash
cd writer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

### 3. 测试

```bash
# 测试 Writer Service 调用 Reviewer Service
curl -X POST http://localhost:8080/api/write-and-review \
  -H "Content-Type: application/json" \
  -d '{"topic": "Spring AI Alibaba"}'
```

## 演示流程

1. **查看 Reviewer Agent Card**
   ```bash
   curl http://localhost:8081/.well-known/agent.json
   ```

2. **Writer Service 生成文章**
   - Writer Agent 根据主题生成文章

3. **通过 A2A 协议调用 Reviewer Service**
   - Writer Service 通过 A2A 协议将文章发送给 Reviewer Service
   - 使用 JSON-RPC 2.0 协议

4. **Reviewer Service 评审文章**
   - Reviewer Agent 对文章进行评审和修改

5. **返回最终结果**
   - Writer Service 返回评审后的文章

## 配置说明

### Reviewer Service 配置
- 端口：8081
- A2A Server Type：JSONRPC
- Agent Name：reviewer-agent

### Writer Service 配置
- 端口：8080
- A2A Client 配置：指向 http://localhost:8081/a2a
- Agent Name：writer-agent

## 文件结构

```
dual-service-demo/
├── README.md
├── reviewer-service/          # Reviewer Service
│   ├── pom.xml
│   ├── src/
│   └── ...
├── writer-service/            # Writer Service
│   ├── pom.xml
│   ├── src/
│   └── ...
└── scripts/
    ├── start-reviewer.sh      # 启动 Reviewer Service
    ├── start-writer.sh        # 启动 Writer Service
    └── test-demo.sh           # 测试脚本
```

