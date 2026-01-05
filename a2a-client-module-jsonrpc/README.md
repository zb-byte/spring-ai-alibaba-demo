### 启动步骤

#### 1. 启动 A2A Server

启动 Agent 服务，该服务将作为 A2A Server 接收来自客户端的请求：

```bash
cd a2a-server-module-jsonrpc
启动 JsonRpcServerApplication
```

Server 将在以下端口启动：
- **HTTP 端口**: 8081
- **Agent Card 端点**: http://127.0.0.1:8081/.well-known/agent.json
- **A2A 消息端点**: http://127.0.0.1:8081/a2a

#### 2. 启动 A2A Client (Writer Agent)

启动 Writer Agent 客户端，该客户端将通过 A2A 协议调用 Reviewer Agent：

```bash
cd a2a-client-module-jsonrpc
启动 JsonRpcClientApplication
```

Client 将在端口 8080 启动，并自动执行 A2A 调用示例。

## 🔄 Agent 间 JSONRPC 通信机制

### 通信流程

Agent 之间通过 **JSONRPC 2.0 协议** 进行通信，具体流程如下：

```
┌─────────────────────────────────────────────────────────────┐
│  1. 服务发现阶段                                               │
│                                                               │
│  Client (Writer Agent)                                        │
│    │                                                          │
│    │ GET http://127.0.0.1:8081/.well-known/agent.json       │
│    │                                                          │
│    ▼                                                          │
│  获取 AgentCard (包含 Agent 元数据、能力、接口信息)            │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 远程调用阶段                                               │
│                                                               │
│  Client (Writer Agent)                                        │
│    │                                                          │
│    │ POST http://127.0.0.1:8081/a2a                          │
│    │ Content-Type: application/json                          │
│    │ Body: JSON-RPC 2.0 格式请求                              │
│    │                                                          │
│    │ {                                                        │
│    │   "jsonrpc": "2.0",                                     │
│    │   "method": "message",                                  │
│    │   "id": "uuid",                                         │
│    │   "params": {                                            │
│    │     "message": {                                        │
│    │       "messageId": "xxx",                               │
│    │       "role": "user",                                   │
│    │       "parts": [{"kind": "text", "text": "..."}]        │
│    │     }                                                    │
│    │   }                                                      │
│    │ }                                                        │
│    │                                                          │
│    ▼                                                          │
│  Server (Reviewer Agent)                                      │
│    │                                                          │
│    │ 处理请求 → 执行 Agent 逻辑 → 返回响应                    │
│    │                                                          │
│    │ {                                                        │
│    │   "jsonrpc": "2.0",                                     │
│    │   "id": "uuid",                                         │
│    │   "result": {                                            │
│    │     "task": { ... } 或 "message": { ... }               │
│    │   }                                                      │
│    │ }                                                        │
│    │                                                          │
│    ▼                                                          │
│  Client 接收响应并解析结果                                     │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件说明

#### 1. Client 端 (Writer Agent)

**配置类** (`AgentConfiguration.java`):
```java
@Bean
public AgentCardProvider reviewerAgentCardProvider() {
    // 从 well-known 端点获取 Agent Card
    String wellKnownUrl = "http://127.0.0.1:8081/.well-known/agent.json";
    return RemoteAgentCardProvider.newProvider(wellKnownUrl);
}

@Bean
public A2aRemoteAgent reviewerRemoteAgent(AgentCardProvider provider) {
    return A2aRemoteAgent.builder()
            .name("reviewer-remote-agent")
            .agentCardProvider(provider)  // 使用 AgentCardProvider 获取远程 Agent 信息
            .instruction("{input}")       // 指令模板
            .outputKey("article")         // 输出键名
            .build();
}
```

**调用示例** (`A2ADemoService.java`):
```java
@Service
public class A2ADemoService {
    private final A2aRemoteAgent a2aRemoteAgent;
    
    public void a2aDemo() {
        // 通过 A2aRemoteAgent 调用远程 Agent
        // Spring AI Alibaba 默认使用 JSONRPC 方式调用远程智能体
        Optional<OverAllState> result = a2aRemoteAgent.invoke("请对以下文章进行评审...");
        // 处理响应结果...
    }
}
```

#### 2. Server 端 (Reviewer Agent)

**配置文件** (`application.yml`):
```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: JSONRPC              # 使用 JSONRPC 传输协议
          address: 127.0.0.1
          port: 8081
          message-url: /a2a         # A2A 消息处理端点
          card:
            name: reviewer-agent
            description: 一个专业的文章评审 Agent
            url: http://127.0.0.1:8081/a2a
            capabilities:
              streaming: true       # 支持流式响应
```

**自动注册的端点**:
- `GET /.well-known/agent.json` - 返回 Agent Card（服务发现）
- `POST /a2a` - 处理 JSONRPC 格式的 A2A 消息请求

### JSONRPC 请求/响应格式

#### 请求格式 (非流式)
```json
{
  "jsonrpc": "2.0",
  "method": "message",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "params": {
    "message": {
      "kind": "message",
      "messageId": "msg-123",
      "role": "user",
      "parts": [
        {
          "kind": "text",
          "text": "请对以下文章进行评审：人工智能是一场新的工业革命"
        }
      ]
    },
    "metadata": {
      "threadId": "thread-123",
      "userId": "user-123"
    }
  }
}
```

#### 响应格式
```json
{
  "jsonrpc": "2.0",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "result": {
    "task": {
      "id": "task-123",
      "status": {
        "state": "COMPLETED"
      },
      "artifacts": [
        {
          "name": "response",
          "parts": [
            {
              "kind": "text",
              "text": "评审结果：..."
            }
          ]
        }
      ]
    }
  }
}
```

### 关键特性

1. **服务发现**: Client 通过 `/.well-known/agent.json` 端点自动发现远程 Agent 的能力和接口信息
2. **协议标准化**: 使用 JSONRPC 2.0 标准协议，确保跨语言、跨平台的互操作性
3. **自动序列化**: Spring AI Alibaba 框架自动处理消息的序列化和反序列化
4. **流式支持**: 支持流式响应（SSE），适用于需要实时返回结果的场景
5. **错误处理**: JSONRPC 标准错误格式，便于客户端处理异常情况