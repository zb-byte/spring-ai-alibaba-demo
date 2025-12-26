# 双服务 A2A 演示指南

## 演示目标

展示两个独立的 Spring Boot 服务通过 A2A 协议进行分布式 Agent 通信。

## 架构图

```
用户请求
   ↓
┌─────────────────────────────────────────┐
│      Writer Service (Port 8080)        │
│  ┌───────────────────────────────────┐ │
│  │  Writer Agent (本地 ReactAgent)   │ │
│  │  生成文章                          │ │
│  └───────────────────────────────────┘ │
│              ↓                          │
│  ┌───────────────────────────────────┐ │
│  │  A2aRemoteAgent (A2A Client)      │ │
│  │  通过 A2A 协议调用 Reviewer       │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
              ↓ A2A Protocol (JSON-RPC 2.0)
              ↓ HTTP POST http://127.0.0.1:8081/a2a
┌─────────────────────────────────────────┐
│    Reviewer Service (Port 8081)        │
│  ┌───────────────────────────────────┐ │
│  │  A2A Server (JSON-RPC Handler)    │ │
│  │  接收 A2A 协议请求                │ │
│  └───────────────────────────────────┘ │
│              ↓                          │
│  ┌───────────────────────────────────┐ │
│  │  Reviewer Agent (本地 ReactAgent)  │ │
│  │  评审和修改文章                    │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
              ↓
        返回评审结果
```

## 启动步骤

### 1. 启动 Reviewer Service（必须先启动）

```bash
cd dual-service-demo
./scripts/start-reviewer.sh
```

或者：

```bash
cd dual-service-demo/reviewer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**验证启动成功：**
```bash
curl http://localhost:8081/.well-known/agent.json
```

### 2. 启动 Writer Service

```bash
cd dual-service-demo
./scripts/start-writer.sh
```

或者：

```bash
cd dual-service-demo/writer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

**验证启动成功：**
```bash
curl http://localhost:8080/api/health
```

### 3. 运行测试

```bash
cd dual-service-demo
./scripts/test-demo.sh
```

## 演示流程详解

### 步骤 1: Writer Agent 生成文章

```
用户请求: {"topic": "Spring AI Alibaba 的优势"}
   ↓
Writer Agent 调用 LLM
   ↓
生成原始文章
```

### 步骤 2: 通过 A2A 协议调用 Reviewer Service

```
Writer Service 构建 A2A 请求:
{
  "jsonrpc": "2.0",
  "method": "a2a.message.send",
  "params": {
    "message": {
      "role": "user",
      "content": "请评审以下文章：..."
    }
  },
  "id": 1
}
   ↓
HTTP POST http://127.0.0.1:8081/a2a
   ↓
Reviewer Service 接收请求
```

### 步骤 3: Reviewer Agent 处理

```
Reviewer Service 解析 A2A 请求
   ↓
调用 Reviewer Agent
   ↓
Reviewer Agent 调用 LLM 评审文章
   ↓
返回评审后的文章
```

### 步骤 4: 返回结果

```
Reviewer Service 返回 A2A 响应
   ↓
Writer Service 解析响应
   ↓
返回给用户
```

## 关键代码说明

### Writer Service - A2A Client 配置

```java
@Bean
public AgentCard reviewerAgentCard() {
    return new AgentCard.Builder()
            .name("reviewer-agent")
            .url(reviewerAgentUrl + "/a2a")  // Reviewer Service 的地址
            .preferredTransport("JSONRPC")
            .build();
}

@Bean
public A2aRemoteAgent reviewerRemoteAgent(AgentCard reviewerAgentCard) {
    return A2aRemoteAgent.builder()
            .agentCard(reviewerAgentCard)  // 使用 Reviewer 的 Agent Card
            .build();
}
```

### Reviewer Service - A2A Server 配置

```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: JSONRPC  # 使用 JSON-RPC 2.0 协议
          message-url: /a2a
          card:
            name: reviewer-agent
            url: http://127.0.0.1:8081/a2a
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

## 给领导演示时的说明

### 1. 架构说明

"这是一个双服务的 A2A 演示，展示了两个独立的服务如何通过 A2A 协议进行通信。"

### 2. 流程说明

"当用户请求写文章时：
1. Writer Service 的 Writer Agent 先生成文章
2. 然后通过 A2A 协议调用 Reviewer Service
3. Reviewer Service 的 Reviewer Agent 对文章进行评审
4. 返回评审后的文章"

### 3. 协议说明

"这里使用的是 JSON-RPC 2.0 协议，但 A2A 还支持 gRPC 和 HTTP+JSON/REST，可以根据场景选择。"

### 4. 分布式说明

"两个服务是完全独立的，可以部署在不同的机器上，通过网络协议通信，这就是真正的分布式 Agent 通信。"

## 故障排查

### 问题 1: Writer Service 无法连接 Reviewer Service

**检查：**
1. Reviewer Service 是否已启动
2. 端口 8081 是否可访问
3. `reviewer.agent.url` 配置是否正确

**测试：**
```bash
curl http://localhost:8081/.well-known/agent.json
```

### 问题 2: 响应为空

**检查：**
1. LLM API Key 是否正确配置
2. 两个服务的日志是否有错误
3. 网络连接是否正常

### 问题 3: 编译错误

**解决：**
```bash
cd reviewer-service && mvn clean install -DskipTests
cd ../writer-service && mvn clean install -DskipTests
```

## 扩展演示

### 1. 切换传输协议

修改 `reviewer-service/src/main/resources/application.yml`：
```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: GRPC  # 或 HTTP+JSON
```

### 2. 添加更多 Agent

可以添加第三个服务，形成更长的协作链：
- Writer Agent → Reviewer Agent → Translator Agent

### 3. 集成 Nacos

启用 Nacos 服务发现，实现动态 Agent 发现和路由。

