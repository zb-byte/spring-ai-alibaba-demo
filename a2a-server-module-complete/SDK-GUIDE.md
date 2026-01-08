# A2A Server SDK 使用指南

一个框架级别的 A2A Server SDK，让用户只需关注业务逻辑，自动支持 REST、gRPC 和 JSON-RPC 三种协议。

## 核心设计

### 设计模式

1. **策略模式**：不同协议的实现策略
2. **工厂模式**：创建协议服务器实例
3. **建造者模式**：构建和配置服务器
4. **适配器模式**：适配用户Agent到SDK接口
5. **模板方法模式**：Agent执行流程模板

### 架构优势

- **极简API**：用户只需实现一个接口
- **自动配置**：Spring Boot自动配置，开箱即用
- **协议无关**：业务逻辑与协议完全解耦
- **灵活配置**：支持配置文件和注解配置
- **统一入口**：单一启动器管理所有协议

## 快速开始

### 1. 添加依赖

在你的项目中添加SDK依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>a2a-server-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 实现 Agent 接口

创建你的Agent类，只需实现 `A2AAgent` 接口：

```java
@Component
public class MyAgent implements A2AAgent<MyAgent.MyContext> {

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
    public MyContext createContext(Map<String, Object> params) {
        return new MyContext();
    }

    @Override
    public AgentResponse execute(String input, MyContext context) {
        // 处理用户输入
        String response = chatClient.prompt()
                .user(input)
                .call()
                .content();

        // 返回响应
        return AgentResponse.builder()
                .content(response)
                .finished(true)
                .build();
    }

    // 自定义上下文类
    public static class MyContext implements AgentContext {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public String getTaskId() {
            return attributes.getOrDefault("taskId", "default").toString();
        }

        @Override
        public String getContextId() {
            return attributes.getOrDefault("contextId", "default").toString();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
```

### 3. 配置 application.yml

```yaml
a2a:
  server:
    auto-start: true          # 自动启动
    rest-enabled: true        # 启用 REST
    rest-port: 7003          # REST 端口
    grpc-enabled: true        # 启用 gRPC
    grpc-port: 9092          # gRPC 端口
    json-rpc-enabled: true    # 启用 JSON-RPC
    json-rpc-port: 7003      # JSON-RPC 端口
    host: localhost          # 服务器主机
```

### 4. 启动应用

```java
@SpringBootApplication
@EnableA2AServer  // 启用 A2A Server
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

就这样！你的Agent现在已经支持三种协议了：

- **REST API**: http://localhost:7003
- **gRPC API**: http://localhost:9092
- **JSON-RPC API**: http://localhost:7003/a2a

## 高级用法

### 1. 使用注解配置

```java
@SpringBootApplication
@EnableA2AServer(
    enableRest = true,
    enableGrpc = true,
    enableJsonRpc = true,
    restPort = 8080,
    grpcPort = 9090,
    jsonRpcPort = 8080
)
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

### 2. 编程式启动

```java
@Component
public class ServerInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private A2AAgent<?> agent;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        A2AServerProperties properties = A2AServerProperties.builder()
                .enableRest(true)
                .restPort(7003)
                .enableGrpc(true)
                .grpcPort(9092)
                .enableJsonRpc(true)
                .jsonRpcPort(7003)
                .build();

        A2AServerBootstrap.bootstrap()
                .agent(agent)
                .applicationContext(applicationContext)
                .properties(properties)
                .start();
    }
}
```

### 3. 选择性启动协议

只启动需要的协议：

```yaml
a2a:
  server:
    rest-enabled: true
    grpc-enabled: false      # 禁用 gRPC
    json-rpc-enabled: false  # 禁用 JSON-RPC
```

```java
A2AServerBootstrap.builder()
    .agent(agent)
    .applicationContext(applicationContext)
    .enableProtocols(ProtocolType.HTTP_REST)  // 只启用 REST
    .build()
    .start();
```

### 4. 自定义Agent能力

```java
@Component
public class AdvancedAgent implements A2AAgent<AdvancedAgent.Context> {

    @Override
    public AgentCapability[] getCapabilities() {
        return new AgentCapability[]{
            AgentCapability.CHAT,
            AgentCapability.STREAMING,
            AgentCapability.TASK_MANAGEMENT
        };
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public AgentResponse execute(String input, Context context) {
        // 流式响应
        if (context.requiresStreaming()) {
            return AgentResponse.builder()
                    .content("初始响应")
                    .finished(false)  // 未完成，可以继续发送
                    .build();
        }

        // 普通响应
        return AgentResponse.builder()
                .content("完整响应")
                .finished(true)
                .metadata(Map.of("source", "advanced-agent"))
                .build();
    }
}
```

## API 文档

### A2AAgent 接口

```java
public interface A2AAgent<C> {
    String getName();                              // Agent 名称
    String getDescription();                       // Agent 描述
    default String getVersion();                   // 版本号
    default AgentCapability[] getCapabilities();   // 能力列表
    C createContext(Map<String, Object> params);  // 创建上下文
    AgentResponse execute(String input, C context); // 执行逻辑
    default boolean supportsStreaming();          // 支持流式
}
```

### AgentResponse

```java
AgentResponse.builder()
    .content("响应内容")
    .finished(true)                    // 是否完成
    .metadata(Map.of("key", "value"))  // 元数据
    .build();
```

### ProtocolType

```java
public enum ProtocolType {
    HTTP_REST,   // REST 协议
    GRPC,        // gRPC 协议
    JSON_RPC     // JSON-RPC 协议
}
```

## 端点说明

### REST 端点

- `GET /.well-known/agent-card.json` - 获取 Agent 卡片
- `POST /v1/message:send` - 发送消息
- `POST /v1/message:stream` - 流式消息
- `GET /v1/tasks/{taskId}` - 获取任务
- `POST /v1/tasks/{taskId}:cancel` - 取消任务

### gRPC 端点

实现 A2A gRPC 协议，使用标准的 gRPC 客户端连接。

### JSON-RPC 端点

- `POST /a2a` - JSON-RPC 端点

## 示例项目

查看 `a2a-server-module-complete` 模块中的 `MyAgent` 类，这是一个完整的实现示例。

## 最佳实践

1. **上下文管理**：使用上下文存储会话状态和中间结果
2. **错误处理**：捕获异常并返回友好的错误消息
3. **元数据**：使用元数据传递额外的信息
4. **流式响应**：对于长时间运行的任务，使用流式响应
5. **能力声明**：准确声明Agent的能力，让客户端知道支持哪些功能

## 故障排除

### Agent 未被自动启动

检查：
1. 是否添加了 `@Component` 注解
2. 是否添加了 `@EnableA2AServer` 注解
3. 配置文件中 `auto-start` 是否为 `true`

### 端口冲突

修改配置文件中的端口：
```yaml
a2a:
  server:
    rest-port: 8080
    grpc-port: 9090
```

### 找不到 Bean

确保组件扫描路径正确：
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.yourapp", "com.example.a2aserver.sdk"})
public class MyApp {
    // ...
}
```

## 性能优化

1. **线程池配置**：调整 `a2aExecutor` 线程池大小
2. **连接池**：配置 HTTP 客户端连接池
3. **缓存**：使用 Spring Cache 缓存常用数据
4. **异步处理**：对于耗时操作使用 `@Async`

## 扩展开发

### 自定义协议实现

```java
public class MyCustomProtocolServer extends AbstractProtocolServer {

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.HTTP_REST;  // 或定义新类型
    }

    @Override
    protected void doStart(AgentCard agentCard) throws Exception {
        // 启动自定义协议服务器
    }

    @Override
    protected void doStop() throws Exception {
        // 停止服务器
    }

    @Override
    protected AgentCard buildAgentCard() {
        // 构建 AgentCard
    }
}
```

### 注册自定义协议

```java
@Component
public class CustomProtocolFactory implements ProtocolServerFactory {
    // 实现 factory 方法
}
```

## 常见问题

**Q: 如何同时支持多个Agent？**

A: 创建多个 Agent 实现，每个 Agent 作为一个独立的 Spring Bean。SDK 会自动检测并启动所有 Agent。

**Q: 如何禁用某个协议？**

A: 在配置文件中设置 `enabled: false`，或在 `@EnableA2AServer` 注解中设置。

**Q: 如何自定义端口？**

A: 在 `application.yml` 中配置，或使用 `@EnableA2AServer` 注解的参数。

**Q: 支持哪些LLM？**

A: 支持所有 Spring AI 兼容的 LLM，包括 OpenAI、Azure OpenAI、通义千问、文心一言等。

## 支持

如有问题，请提交 Issue 或查看项目文档。
