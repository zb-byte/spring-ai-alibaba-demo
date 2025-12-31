# A2A gRPC Client Demo

基于 A2A Java SDK v0.3.3.Final 实现的 gRPC 客户端演示模块。

## 功能特性

- ✅ 真实的 A2A gRPC 客户端实现
- ✅ 基于 A2A Java SDK v0.3.3.Final
- ✅ Spring Boot 集成
- ✅ gRPC Channel 管理
- ✅ Agent Card 获取 (HTTP + gRPC)
- ✅ 消息发送/接收
- ✅ 事件处理 (MessageEvent, TaskEvent, TaskUpdateEvent)
- ✅ Web 界面测试工具

## 技术栈

- Spring Boot 3.x
- A2A Java SDK v0.3.3.Final
- gRPC 1.64.0
- Java 17+

## 快速开始

### 1. 启动 A2A Server

```bash
cd ../a2a-server-module-grpc
mvn spring-boot:run
```

Server 将在以下端口启动：
- HTTP: 7002
- gRPC: 9090

### 2. 启动 A2A Client

```bash
cd ../a2a-client-module-grpc
mvn spring-boot:run
```

Client 将在端口 7001 启动。

### 3. 访问 Web 界面

打开浏览器访问: http://localhost:7001

## API 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/test-connection` | GET | 测试 HTTP 连接 |
| `/api/test-grpc` | GET | 测试 gRPC 连接 |
| `/api/init-client` | POST | 初始化 A2A gRPC 客户端 |
| `/api/agent-card` | GET | 通过 HTTP 获取 Agent Card |
| `/api/agent-card-a2a` | GET | 通过 gRPC 获取 Agent Card |
| `/api/send-message` | POST | 通过 HTTP 发送消息 |
| `/api/send-message-a2a` | GET | 通过 gRPC 发送消息 |

## 配置

`application.yml`:

```yaml
server:
  port: 7001

a2a:
  server:
    host: localhost
    port: 7002
    grpc-port: 9090
    agent-card-url: "http://localhost:7002/.well-known/agent-card.json"
```

## 核心组件

### A2AClientService

真实的 A2A gRPC 客户端服务，提供：

- `initializeClient()` - 初始化 gRPC 连接和 A2A Client
- `sendMessage(String)` - 发送消息到 A2A Server
- `getAgentCard()` - 获取 Agent Card
- `testConnection()` - 测试连接状态

### 使用示例

```java
@Autowired
private A2AClientService a2aClientService;

// 发送消息
CompletableFuture<String> response = a2aClientService.sendMessage("Hello, Agent!");
response.thenAccept(result -> System.out.println("Response: " + result));

// 获取 Agent Card
CompletableFuture<String> agentCard = a2aClientService.getAgentCard();
agentCard.thenAccept(card -> System.out.println("Agent Card: " + card));
```

## A2A SDK v0.3.3.Final API 说明

### 类型和方法

| 类 | 类型 | 方法风格 |
|----|------|----------|
| `Message` | 普通类 | getter: `getParts()`, `getRole()` |
| `Task` | 普通类 | getter: `getId()`, `getStatus()`, `getArtifacts()` |
| `Artifact` | record | record: `parts()`, `name()` |
| `TextPart` | 普通类 | getter: `getText()` |
| `AgentCard` | record | record: `name()`, `capabilities()` |

### Builder 模式

v0.3.3.Final 使用 `new Builder()` 构造器模式：

```java
// 创建消息
Message message = new Message.Builder()
    .role(Message.Role.USER)
    .parts(List.of(new TextPart("Hello")))
    .build();

// 创建 AgentCard
AgentCard card = new AgentCard.Builder()
    .name("My Agent")
    .capabilities(new AgentCapabilities.Builder()
        .streaming(true)
        .build())
    .build();
```

## 项目结构

```
a2a-client-module-grpc/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/a2aclient/
    │   ├── A2AClientService.java      # gRPC 客户端服务
    │   ├── A2aClientModuleApplication.java
    │   └── controller/
    │       └── ClientDemoController.java  # REST 控制器
    └── resources/
        ├── application.yml
        └── static/
            └── index.html             # Web 测试界面
```

## 依赖

```xml
<dependencies>
    <!-- A2A SDK -->
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-client</artifactId>
        <version>0.3.3.Final</version>
    </dependency>
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-client-transport-grpc</artifactId>
        <version>0.3.3.Final</version>
    </dependency>
    
    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.64.0</version>
    </dependency>
</dependencies>
```
