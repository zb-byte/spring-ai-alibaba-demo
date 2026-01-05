# A2A REST Client Demo

基于 A2A Java SDK v0.3.3.Final 构建的 REST Client Demo，演示如何通过 HTTP REST 协议与 A2A Server 进行通信。

## 🚀 快速开始

### 1. 启动 A2A Server

首先启动 A2A Server 服务：

```bash
cd a2a-server-module-rest
mvn spring-boot:run
```

或者直接运行主类：

```bash
mvn clean package -DskipTests
java -cp target/classes:target/dependency/* com.example.a2aserver.RestServerApplication
```

Server 将在端口 **7002** 启动，并自动注册以下 REST 端点：
- `GET /.well-known/agent.json` - Agent Card（服务发现）
- `POST /message:send` - 发送消息（同步）
- `POST /message:stream` - 发送消息（流式，SSE）
- `GET /tasks/{taskId}` - 获取任务状态
- `POST /tasks/{taskId}:cancel` - 取消任务

### 2. 启动 A2A Client

在另一个终端启动 A2A Client：

```bash
cd a2a-client-module-rest
mvn spring-boot:run
```

或者直接运行主类：

```bash
mvn clean package -DskipTests
java -cp target/classes:target/dependency/* com.example.a2aclient.RestClientApplication
```

**注意**：Client 启动后会：
1. 自动连接到 A2A Server（地址配置在 `application.yml` 中的 `a2a.server.url`）
2. 获取 Agent Card 并打印到日志
3. 执行完成后自动退出

启动日志示例：
```
Initializing A2A REST Client for server: http://localhost:7002
Fetching agent card from: http://localhost:7002
Agent card fetched: Spring AI Chat Agent
get agent card: AgentCard{name='Spring AI Chat Agent', ...}
```

### 3. 测试通信

#### 方式一：修改代码进行测试

修改 `A2aClientModuleApplication.java` 的 `main` 方法，调用不同的测试方法：

```java
public static void main(String[] args) {
    ApplicationContext context = SpringApplication.run(A2aClientModuleApplication.class, args);
    RestDemo restDemo = context.getBean(RestDemo.class);
    
    // 测试获取 Agent Card
    restDemo.getAgentCard();
    
    // 测试发送同步消息
    restDemo.sendMessage("你好，请介绍一下你自己");
    
    // 测试发送流式消息（需要修改 RestDemo 以支持流式测试）
    // restDemo.sendMessageStreaming("请写一首关于春天的诗");
    
    System.exit(SpringApplication.exit(context, () -> 0));
}
```

#### 方式二：使用 A2ARestClient 编程方式测试

创建测试类或修改现有代码，直接使用 `A2ARestClient`：

```java
@Autowired
private A2ARestClient a2aClient;

// 获取 Agent Card
AgentCard card = a2aClient.fetchAgentCard();

// 发送同步消息
EventKind result = a2aClient.sendMessage("你好，请介绍一下你自己");

// 发送流式消息
a2aClient.sendMessageStreaming("请写一首关于春天的诗", event -> {
    if (event instanceof Message message) {
        System.out.println("收到消息: " + extractText(message));
    } else if (event instanceof Task task) {
        System.out.println("任务状态: " + task.getStatus().state());
    }
});

// 获取任务状态
Task task = a2aClient.getTask(taskId);

// 取消任务
Task canceledTask = a2aClient.cancelTask(taskId);
```

#### 方式三：使用单元测试

创建 JUnit 测试类进行测试：

```java
@SpringBootTest
class A2ARestClientTest {
    
    @Autowired
    private A2ARestClient a2aClient;
    
    @Test
    void testFetchAgentCard() throws Exception {
        AgentCard card = a2aClient.fetchAgentCard();
        assertNotNull(card);
    }
    
    @Test
    void testSendMessage() throws Exception {
        EventKind result = a2aClient.sendMessage("测试消息");
        assertNotNull(result);
    }
}
```

## 📡 A2A 协议通过 REST 通信机制

### 通信架构

A2A（Agent-to-Agent）协议定义了智能体之间标准化的通信规范。本示例使用 **HTTP REST** 作为传输层，将 A2A 协议消息封装在 HTTP 请求/响应中。

```
┌─────────────────────────────────────────────────────────────┐
│  Client (A2aClientModuleApplication)                         │
│                                                               │
│  A2A Protocol Layer                                          │
│    ├── Message (用户消息)                                     │
│    ├── Task (任务状态)                                        │
│    └── Event (事件流)                                         │
│         │                                                      │
│         ▼                                                      │
│  REST Transport Layer (RestTransport)                        │
│    ├── HTTP Request (JSON/Protobuf)                          │
│    └── HTTP Response (JSON/Protobuf)                         │
└─────────────────────────────────────────────────────────────┘
                        │
                        │ HTTP REST
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Server (A2aServerModuleApplication)                         │
│                                                               │
│  REST Transport Layer (RestHandler)                          │
│    ├── HTTP Request (JSON/Protobuf)                          │
│    └── HTTP Response (JSON/Protobuf)                         │
│         │                                                      │
│         ▼                                                      │
│  A2A Protocol Layer                                          │
│    ├── RequestHandler (处理 A2A 请求)                         │
│    ├── AgentExecutor (执行 Agent 逻辑)                        │
│    └── EventQueue (事件队列管理)                              │
└─────────────────────────────────────────────────────────────┘
```

### 1. 服务发现阶段

Client 首先通过 **Agent Card** 端点发现 Server 的能力和接口信息：

**Client 端代码** (`A2ARestClient.java`):
```java
public AgentCard fetchAgentCard() throws Exception {
    A2ACardResolver resolver = new A2ACardResolver(serverUrl);
    this.agentCard = resolver.getAgentCard();
    this.transport = new RestTransport(agentCard);
    return agentCard;
}
```

**HTTP 请求**:
```http
GET http://localhost:7002/.well-known/agent.json
```

**HTTP 响应**:
```json
{
  "name": "Spring AI Chat Agent",
  "description": "A Spring AI powered A2A Agent",
  "version": "1.0.0",
  "url": "http://localhost:7002",
  "capabilities": {
    "streaming": true,
    "pushNotifications": false
  },
  "preferredTransport": "REST",
  "additionalInterfaces": [
    {
      "transport": "REST",
      "url": "http://localhost:7002"
    }
  ]
}
```

### 2. 消息发送（同步模式）

Client 发送用户消息，Server 处理并返回完整的任务结果。

**Client 端代码**:
```java
Message message = new Message.Builder()
    .messageId(UUID.randomUUID().toString())
    .role(Message.Role.USER)
    .contextId(UUID.randomUUID().toString())
    .parts(List.of(new TextPart("你好")))
    .build();

MessageSendParams params = new MessageSendParams(message, null, null);
EventKind result = transport.sendMessage(params, null);
```

**HTTP 请求**:
```http
POST http://localhost:7002/message:send
Content-Type: application/json

{
  "message": {
    "messageId": "msg-123",
    "role": "user",
    "contextId": "ctx-123",
    "parts": [
      {
        "kind": "text",
        "text": "你好"
      }
    ]
  }
}
```

**HTTP 响应**:
```json
{
  "task": {
    "id": "task-123",
    "contextId": "ctx-123",
    "status": {
      "state": "COMPLETED"
    },
    "artifacts": [
      {
        "artifactId": "art-123",
        "name": "response",
        "parts": [
          {
            "kind": "text",
            "text": "你好！我是 AI 助手..."
          }
        ]
      }
    ]
  }
}
```

### 3. 消息发送（流式模式）

Client 发送消息，Server 通过 **Server-Sent Events (SSE)** 流式返回响应。

**Client 端代码**:
```java
transport.sendMessageStreaming(params, event -> {
    // 处理流式事件
    if (event instanceof Message message) {
        // 收到消息片段
    } else if (event instanceof Task task) {
        // 收到任务更新
    }
}, error -> {
    // 处理错误
}, null);
```

**HTTP 请求**:
```http
POST http://localhost:7002/message:stream
Content-Type: application/json

{
  "message": {
    "messageId": "msg-456",
    "role": "user",
    "parts": [{"kind": "text", "text": "请写一首诗"}]
  }
}
```

**HTTP 响应** (SSE 流):
```
Content-Type: text/event-stream

data: {"message": {"role": "agent", "parts": [{"kind": "text", "text": "春"}]}}
data: {"message": {"role": "agent", "parts": [{"kind": "text", "text": "风"}]}}
data: {"message": {"role": "agent", "parts": [{"kind": "text", "text": "拂"}]}}
...
data: {"task": {"id": "task-456", "status": {"state": "COMPLETED"}}}
```

**Server 端实现** (`A2ARestController.java`):
```java
@PostMapping(value = "/message:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sendStreamingMessage(@RequestBody String body) {
    SseEmitter emitter = new SseEmitter(30000L);
    ServerCallContext context = new ServerCallContext(...);
    RestHandler.HTTPRestResponse response = restHandler.sendStreamingMessage(body, context);
    
    // 将 Flow.Publisher<String> 转换为 SSE 流
    publisher.subscribe(new Flow.Subscriber<String>() {
        @Override
        public void onNext(String item) {
            emitter.send(SseEmitter.event().data(item));
        }
        // ...
    });
    return emitter;
}
```

### 4. 协议转换层

A2A SDK 的 `RestHandler` 和 `RestTransport` 负责在 A2A 协议和 HTTP REST 之间进行转换：

**Server 端** (`RestHandler`):
- 接收 HTTP JSON 请求
- 使用 Protocol Buffers 工具转换为 A2A 协议对象
- 调用 `RequestHandler` 处理 A2A 请求
- 将 A2A 响应转换为 JSON 返回

**Client 端** (`RestTransport`):
- 将 A2A 协议对象（Message, Task 等）转换为 HTTP 请求
- 发送 HTTP 请求到 Server
- 解析 HTTP 响应为 A2A 协议对象

### 5. 任务管理

Client 可以查询和取消任务：

**获取任务状态**:
```http
GET http://localhost:7002/tasks/{taskId}?historyLength=0
```

**取消任务**:
```http
POST http://localhost:7002/tasks/{taskId}:cancel
```

## 🔑 核心组件

### Client 端

- **A2aClientModuleApplication**: Spring Boot 主应用类，启动时执行测试操作后退出
- **A2ARestClient**: 封装 `RestTransport`，提供 A2A 协议调用接口
- **RestDemo**: 演示服务，提供测试方法（可通过编程方式调用）

### Server 端

- **A2aServerModuleApplication**: Spring Boot 主应用类
- **A2ARestController**: REST 端点控制器，使用 `RestHandler` 处理请求
- **A2AAgentExecutor**: 实现 `AgentExecutor` 接口，使用 Spring AI 处理消息
- **RequestHandler**: A2A 请求处理器
- **SimpleQueueManager**: 事件队列管理器

## 📝 配置说明

### Client 配置 (`application.yml`)

```yaml
server:
  port: 7001

spring:
  application:
    name: a2a-client-rest-demo

# A2A Server Configuration
a2a:
  server:
    url: http://localhost:7002  # A2A Server 地址

logging:
  level:
    root: INFO
    com.example.a2aclient: DEBUG
    io.a2a: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**配置说明**：
- `server.port`: Client 应用端口（虽然当前实现启动后即退出，但保留端口配置以便未来扩展）
- `a2a.server.url`: A2A Server 的地址，Client 会连接到此地址获取 Agent Card 和发送消息
- `logging.level`: 日志级别配置，建议开发时设置为 DEBUG 以便查看详细的通信日志

### Server 配置 (`application.yml`)

```yaml
server:
  port: 7002

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL}
      chat:
        options:
          model: ${OPENAI_MODEL}

agent:
  name: Spring AI Chat Agent
  description: A Spring AI powered A2A Agent
```

## 🎯 关键特性

1. **标准化协议**: 使用 A2A 协议规范，确保跨平台、跨语言互操作性
2. **REST 传输**: 基于 HTTP REST，易于集成和调试
3. **流式支持**: 支持 Server-Sent Events (SSE) 实现实时响应
4. **服务发现**: 通过 Agent Card 自动发现服务能力和接口
5. **协议转换**: SDK 自动处理 A2A 协议与 HTTP REST 的转换

## 📚 相关文档

- [A2A 协议规范](https://github.com/a2aproject/a2a-spec)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java)
