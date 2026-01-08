# A2A Server SDK - 框架级多协议 Agent 服务器

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21)
[![A2A SDK](https://img.shields.io/badge/A2A%20SDK-0.3.3-blue.svg)](https://github.com/google/a2a)

一个**框架级别**的 A2A Server SDK，让你只需关注业务逻辑，自动暴露 **REST**、**gRPC** 和 **JSON-RPC** 三种协议。

## ✨ 核心特性

- **🎯 极简设计** - 只需实现一个接口，自动支持三种协议
- **🔌 协议无关** - 业务逻辑与通信协议完全解耦
- **⚙️ 自动配置** - Spring Boot 自动配置，开箱即用
- **🎨 设计模式** - 策略、工厂、建造者等多种设计模式
- **🚀 灵活启动** - 支持配置文件、注解、编程式配置
- **🔧 生产就绪** - 完整的错误处理、日志、监控支持

## 📋 架构设计

### 设计模式

```
┌─────────────────────────────────────────────────────────┐
│                   A2AServerBootstrap                    │
│                    (建造者模式)                          │
│  ┌─────────────┬──────────────┬─────────────────────┐  │
│  │             │              │                     │  │
│  │  REST       │    gRPC      │    JSON-RPC         │  │
│  │  Protocol   │    Protocol  │    Protocol        │  │
│  │  Server     │    Server    │    Server          │  │
│  │             │              │                     │  │
│  └─────────────┴──────────────┴─────────────────────┘  │
│         ↕              ↕               ↕                │
│  ┌──────────────────────────────────────────────────┐ │
│  │       ProtocolServerFactory (工厂模式)            │ │
│  └──────────────────────────────────────────────────┘ │
│         ↕                                               │
│  ┌──────────────────────────────────────────────────┐ │
│  │     A2AAgentExecutorAdapter (适配器模式)          │ │
│  └──────────────────────────────────────────────────┘ │
│         ↕                                               │
│  ┌──────────────────────────────────────────────────┐ │
│  │         A2AAgent (用户实现)                       │ │
│  └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 核心组件

1. **A2AAgent** - 用户只需实现这个接口
2. **ProtocolServer** - 协议服务器抽象
3. **A2AServerBootstrap** - 统一启动器
4. **ProtocolServerFactory** - 服务器工厂
5. **A2AServerAutoConfiguration** - 自动配置

## 🚀 快速开始

### 1. 实现 Agent

```java
@Component
public class MyAgent implements A2AAgent<MyAgent.Context> {

    private final ChatClient chatClient;

    public MyAgent(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个友好的AI助手")
                .build();
    }

    @Override
    public String getName() {
        return "My Agent";
    }

    @Override
    public String getDescription() {
        return "我的第一个A2A Agent";
    }

    @Override
    public Context createContext(Map<String, Object> params) {
        return new Context();
    }

    @Override
    public AgentResponse execute(String input, Context context) {
        String response = chatClient.prompt()
                .user(input)
                .call()
                .content();

        return AgentResponse.builder()
                .content(response)
                .finished(true)
                .build();
    }

    static class Context implements AgentContext {
        // 实现上下文接口
    }
}
```

### 2. 配置

```yaml
a2a:
  server:
    auto-start: true
    rest-enabled: true
    rest-port: 7003
    grpc-enabled: true
    grpc-port: 9092
    json-rpc-enabled: true
```

### 3. 启动

```java
@SpringBootApplication
@EnableA2AServer
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

完成！你的Agent现在支持三种协议：

- **REST**: http://localhost:7003
- **gRPC**: http://localhost:9092
- **JSON-RPC**: http://localhost:7003/a2a

## 📚 文档

- [SDK 使用指南](SDK-GUIDE.md) - 详细的使用文档
- [API 文档](https://example.com/api) - API 参考
- [示例代码](src/main/java/com/example/a2aserver/example) - 完整示例

## 🎯 使用场景

### 场景 1: 只启用 REST

```yaml
a2a:
  server:
    rest-enabled: true
    grpc-enabled: false
    json-rpc-enabled: false
```

### 场景 2: 自定义端口

```yaml
a2a:
  server:
    rest-port: 8080
    grpc-port: 9090
```

### 场景 3: 编程式启动

```java
A2AServerBootstrap.builder()
    .agent(myAgent)
    .applicationContext(context)
    .enableProtocols(ProtocolType.HTTP_REST, ProtocolType.GRPC)
    .build()
    .start();
```

## 🔌 协议端点

### REST API

- `GET /.well-known/agent-card.json` - Agent 卡片
- `POST /v1/message:send` - 发送消息
- `POST /v1/message:stream` - 流式消息
- `GET /v1/tasks/{taskId}` - 获取任务

### gRPC API

使用 A2A gRPC 协议，支持所有标准 gRPC 客户端。

### JSON-RPC API

- `POST /a2a` - JSON-RPC 端点

## 🛠️ 技术栈

- **Java 21**
- **Spring Boot 3.3.1**
- **Spring AI 1.0.0-M6**
- **A2A SDK 0.3.3.Final**
- **gRPC 1.64.0**

## 📦 项目结构

```
a2a-server-module-complete/
├── sdk/                                    # SDK 核心代码
│   ├── agent/                              # Agent 接口
│   │   └── A2AAgent.java
│   ├── protocol/                           # 协议抽象
│   │   ├── ProtocolType.java
│   │   ├── ProtocolServer.java
│   │   └── impl/
│   │       ├── AbstractProtocolServer.java
│   │       ├── RestProtocolServer.java
│   │       ├── GrpcProtocolServer.java
│   │       └── JsonRpcProtocolServer.java
│   ├── server/                             # 服务器启动
│   │   ├── A2AServerBootstrap.java
│   │   └── DefaultProtocolServerFactory.java
│   └── config/                             # 自动配置
│       ├── EnableA2AServer.java
│       ├── A2AServerAutoConfiguration.java
│       └── A2AServerProperties.java
├── example/                                # 示例代码
│   └── MyAgent.java
├── config/                                 # 旧配置（兼容）
├── resources/
│   └── application.yml
└── README.md
```

## 🎓 最佳实践

1. **单一职责** - 每个 Agent 只做一件事
2. **上下文管理** - 使用上下文存储会话状态
3. **错误处理** - 捕获并返回友好的错误信息
4. **元数据** - 使用元数据传递额外信息
5. **能力声明** - 准确声明 Agent 能力

## 🔍 常见问题

**Q: 如何禁用某个协议？**
```yaml
a2a:
  server:
    grpc-enabled: false  # 禁用 gRPC
```

**Q: 如何自定义端口？**
```yaml
a2a:
  server:
    rest-port: 8080
```

**Q: 如何支持多个 Agent？**
创建多个实现了 `A2AAgent` 接口的类，SDK 会自动启动所有 Agent。

**Q: 支持哪些 LLM？**
支持所有 Spring AI 兼容的 LLM，包括 OpenAI、通义千问、文心一言等。

## 📄 许可证

Apache License 2.0

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

如有问题，请提交 Issue 或联系维护者。

---

**Made with  by wzb **
