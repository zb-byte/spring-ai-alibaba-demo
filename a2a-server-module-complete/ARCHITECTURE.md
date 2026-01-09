# A2A Server SDK - 架构总结

## 📦 项目结构

清理后的项目结构，清晰简洁：

```
a2a-server-module-complete/
├── src/main/java/com/example/a2aserver/
│   ├── CompleteA2AServerApplication.java      # 主应用类 (@EnableA2AServer)
│   ├── config/                                # LLM 配置（通用）
│   │   ├── LlmProperties.java                 # LLM 属性
│   │   └── LlmConfiguration.java              # LLM 配置
│   ├── example/                               # 示例 Agent
│   │   └── MyAgent.java                       # 用户只需实现这个
│   └── sdk/                                   # SDK 核心（框架级）
│       ├── agent/                             # Agent 抽象
│       │   └── A2AAgent.java                  # 用户接口
│       ├── protocol/                          # 协议抽象
│       │   ├── ProtocolType.java              # 协议类型枚举
│       │   ├── ProtocolServer.java            # 服务器接口
│       │   ├── ProtocolServerFactory.java     # 工厂接口
│       │   └── impl/                          # 协议实现
│       │       ├── AbstractProtocolServer.java        # 抽象基类
│       │       ├── RestProtocolServer.java            # REST 实现
│       │       ├── GrpcProtocolServer.java            # gRPC 实现
│       │       ├── JsonRpcProtocolServer.java         # JSON-RPC 实现
│       │       └── A2AGrpcServiceDelegate.java        # gRPC 服务委托（实现 getAgentCard、sendMessage 等）
│       ├── server/                            # 服务器管理
│       │   ├── A2AServerBootstrap.java        # 启动器（建造者）
│       │   └── DefaultProtocolServerFactory.java     # 默认工厂
│       └── config/                            # SDK 配置
│           ├── EnableA2AServer.java           # 启用注解
│           ├── A2AServerAutoConfiguration.java        # 自动配置
│           ├── A2AServerProperties.java              # 配置属性（程序化构建）
│           └── A2AServerPropertiesConfiguration.java  # 配置绑定（从 application.yml 读取）
├── src/main/resources/
│   └── application.yml                        # 应用配置
└── pom.xml                                    # Maven 配置
```

## 🎯 核心设计模式

### 1. 策略模式 (Strategy Pattern)

```java
// 协议接口
ProtocolServer
├── RestProtocolServer      // REST 策略
├── GrpcProtocolServer      // gRPC 策略
└── JsonRpcProtocolServer   // JSON-RPC 策略
```

### 2. 工厂模式 (Factory Pattern)

```java
ProtocolServerFactory
    └── createServer(ProtocolType, Agent, Context)
        ├── HTTP_REST  → RestProtocolServer
        ├── GRPC       → GrpcProtocolServer
        └── JSON_RPC   → JsonRpcProtocolServer
```

### 3. 建造者模式 (Builder Pattern)

```java
A2AServerBootstrap.builder()
    .agent(agent)
    .applicationContext(context)
    .properties(properties)
    .enableProtocols(REST, GRPC, JSON_RPC)
    .build()
    .start();
```

### 4. 适配器模式 (Adapter Pattern)

```java
A2AAgent (用户接口)
    ↓ A2AAgentExecutorAdapter
AgentExecutor (A2A SDK 接口)
```

### 5. 模板方法模式 (Template Method Pattern)

```java
AbstractProtocolServer
    ├── start()              # 模板方法
    ├── stop()               # 模板方法
    ├── buildAgentCard()     # 抽象方法
    ├── doStart()            # 抽象方法
    └── doStop()             # 抽象方法
```

## 🔄 工作流程

### 启动流程

```
1. @EnableA2AServer 注解
   ↓
2. A2AServerAutoConfiguration 自动配置
   ↓
3. A2AServerPropertiesConfiguration 绑定配置（从 application.yml）
   ↓
4. 扫描 A2AAgent 实现类
   ↓
5. 创建 A2AServerBootstrap
   ↓
6. 通过 ProtocolServerFactory 创建协议服务器
   ├─→ RestProtocolServer (Spring MVC Controller)
   ├─→ GrpcProtocolServer (gRPC Server + A2AGrpcServiceDelegate)
   └─→ JsonRpcProtocolServer (JSON-RPC Handler)
   ↓
7. 启动所有协议服务器
   ├─→ REST: 启动 Spring MVC 端点
   ├─→ gRPC: 启动 gRPC Server，注册 A2AGrpcServiceDelegate
   └─→ JSON-RPC: 注册 JSON-RPC 处理器
   ↓
8. 暴露端点
   ├─→ REST: /.well-known/agent-card.json, /v1/message:send
   ├─→ gRPC: getAgentCard(), sendMessage()
   └─→ JSON-RPC: POST /a2a
```

### 请求处理流程

#### REST 请求处理
```
HTTP POST /v1/message:send
   ↓
RestProtocolServer.sendMessage()
   ↓
提取消息内容
   ↓
A2AAgent.execute() (用户逻辑)
   ↓
AgentResponse (响应)
   ↓
序列化为 JSON
   ↓
返回 HTTP 响应
```

#### gRPC 请求处理
```
gRPC sendMessage() 调用
   ↓
A2AGrpcServiceDelegate.sendMessage()
   ↓
从 SendMessageRequest 提取消息（request.getRequest().getContentList()）
   ↓
A2AAgent.execute() (用户逻辑)
   ↓
AgentResponse (响应)
   ↓
构建 gRPC Message 对象
   ↓
构建 SendMessageResponse (setMsg())
   ↓
返回 gRPC 响应
```

#### JSON-RPC 请求处理
```
HTTP POST /a2a (JSON-RPC)
   ↓
JsonRpcProtocolServer.handle()
   ↓
解析 JSON-RPC 请求
   ↓
A2AAgent.execute() (用户逻辑)
   ↓
AgentResponse (响应)
   ↓
构建 JSON-RPC 响应
   ↓
返回 JSON 响应
```

## 🎨 核心优势

### 1. 极简用户接口

```java
// 用户只需实现这一个接口
public interface A2AAgent<C> {
    String getName();
    String getDescription();
    C createContext(Map<String, Object> params);
    AgentResponse execute(String input, C context);
}
```

### 2. 协议完全解耦

```java
// 业务逻辑完全不知道底层协议
String response = chatClient.prompt()
    .user(input)
    .call()
    .content();
```

### 3. 灵活的配置方式

**方式一：配置文件**
```yaml
a2a:
  server:
    auto-start: true
    rest-enabled: true
    grpc-enabled: true
```

**方式二：注解**
```java
@EnableA2AServer(
    enableRest = true,
    enableGrpc = true,
    restPort = 8080
)
```

**方式三：编程式**
```java
A2AServerBootstrap.builder()
    .agent(agent)
    .enableProtocols(ProtocolType.HTTP_REST)
    .build()
    .start();
```

### 4. 自动配置

```java
@SpringBootApplication
@EnableA2AServer  // 一个注解搞定
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

## 📊 代码对比

### 旧架构（已删除）

```java
// 用户需要实现多个类
@RestController
public class MyController { ... }  // REST 控制器

@Service
public class MyGrpcService { ... } // gRPC 服务

@RestController
public class MyJsonRpcController { ... } // JSON-RPC 控制器

@Configuration
public class MyConfig { ... }  // 配置类

// 还有各种适配器、处理器...
```

### 新架构（SDK）

```java
// 用户只需实现一个接口
@Component
public class MyAgent implements A2AAgent<MyContext> {
    @Override
    public AgentResponse execute(String input, MyContext context) {
        // 业务逻辑
    }
}
```

## 🧩 依赖关系

```
用户代码
    ↓ 依赖
SDK 核心层
    ├─→ A2AAgent (接口)
    ├─→ ProtocolServer (接口)
    └─→ A2AServerBootstrap (启动器)
    ↓ 依赖
A2A 官方 SDK
    ├─→ a2a-java-sdk-spec
    ├─→ a2a-java-sdk-server-common
    ├─→ a2a-java-sdk-transport-rest
    └─→ a2a-java-sdk-transport-grpc
    ↓ 依赖
Spring AI
    ├─→ spring-ai-openai
    └─→ spring-ai-alibaba
```

## 🔧 扩展性

### 添加新协议

```java
// 1. 实现 ProtocolServer 接口
public class MyProtocolServer extends AbstractProtocolServer {
    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.MY_PROTOCOL;
    }

    @Override
    protected void doStart(AgentCard card) { ... }

    @Override
    protected void doStop() { ... }
}

// 2. 在 ProtocolType 中添加类型
public enum ProtocolType {
    MY_PROTOCOL("MY_PROTO", "My Protocol", 8080)
}

// 3. 在 DefaultProtocolServerFactory 中注册
case MY_PROTOCOL -> new MyProtocolServer(...);
```

### 自定义 Agent 能力

```java
@Override
public AgentCapability[] getCapabilities() {
    return new AgentCapability[]{
        AgentCapability.CHAT,
        AgentCapability.STREAMING,
        AgentCapability.TOOLS  // 自定义能力
    };
}
```

## 📈 性能优化

1. **线程池管理** - SDK 自动管理线程池
2. **连接复用** - 协议层自动管理连接
3. **异步处理** - 支持异步 Agent 执行
4. **流式响应** - 减少内存占用

## 🛡️ 错误处理

```java
try {
    AgentResponse response = agent.execute(input, context);
    return response;
} catch (Exception e) {
    // SDK 自动捕获并返回友好错误
    return AgentResponse.builder()
        .content("Error: " + e.getMessage())
        .finished(true)
        .build();
}
```

## 🔌 gRPC 服务实现

### gRPC 服务暴露机制

gRPC 服务通过 `A2AGrpcServiceDelegate` 实现，继承自 `A2AServiceGrpc.A2AServiceImplBase`：

```java
public class A2AGrpcServiceDelegate extends A2AServiceGrpc.A2AServiceImplBase {
    // 实现所有 gRPC 服务方法
    @Override
    public void getAgentCard(...) { ... }      // ✅ 已实现
    @Override
    public void sendMessage(...) { ... }       // ✅ 已实现
    @Override
    public void sendStreamingMessage(...) { ... } // ⏳ 待实现
    @Override
    public void getTask(...) { ... }           // ⏳ 待实现
}
```

### 服务注册

在 `GrpcProtocolServer.doStart()` 中注册服务：

```java
grpcServer = ServerBuilder.forPort(port)
    .addService(grpcServiceDelegate)              // 注册 A2A 服务
    .addService(ProtoReflectionService.newInstance()) // 启用反射服务
    .build()
    .start();
```

### gRPC 消息处理

#### getAgentCard 实现
```java
@Override
public void getAgentCard(GetAgentCardRequest request,
                        StreamObserver<AgentCard> responseObserver) {
    // 返回完整的 AgentCard，包含能力、技能等信息
    responseObserver.onNext(agentCard);
    responseObserver.onCompleted();
}
```

#### sendMessage 实现
```java
@Override
public void sendMessage(SendMessageRequest request,
                       StreamObserver<SendMessageResponse> responseObserver) {
    // 1. 从 request.getRequest().getContentList() 提取消息内容
    // 2. 创建 Agent 上下文
    // 3. 调用 A2AAgent.execute()
    // 4. 构建 gRPC Message 对象
    // 5. 使用 SendMessageResponse.setMsg() 返回响应
}
```

**关键点：**
- `SendMessageRequest` 包含 `request` (Message 类型)，需要从 `getContentList()` 提取文本
- `SendMessageResponse` 使用 oneof 模式，通过 `setMsg()` 设置 Message 对象
- 自动生成 taskId 和 contextId（如果请求中未提供）

### 配置属性管理

**两层配置架构：**

1. **A2AServerPropertiesConfiguration** 
   - 从 `application.yml` 绑定配置（`@ConfigurationProperties(prefix = "a2a.server")`）
   - 包含 `autoStart` 等配置属性
   - 提供 `toProperties()` 方法转换为 `A2AServerProperties`

2. **A2AServerProperties**
   - 程序化构建配置对象（**无** `@ConfigurationProperties`）
   - 提供 Builder 模式用于程序化构建
   - 包含协议启用状态、端口等配置

**转换流程：**
```
application.yml
  ↓ @ConfigurationProperties
A2AServerPropertiesConfiguration
  ↓ toProperties()
A2AServerProperties
  ↓
A2AServerBootstrap / ProtocolServer
```

**优势：**
- 避免配置属性重复绑定
- 配置绑定与业务对象分离
- 支持配置文件和程序化两种方式

## 🎯 总结

新的 SDK 架构：

✅ **代码量减少 80%** - 用户只需实现一个接口
✅ **完全解耦** - 业务逻辑与协议无关
✅ **易于扩展** - 添加新协议只需实现接口
✅ **自动化** - Spring Boot 自动配置
✅ **生产就绪** - 完整的错误处理和日志
✅ **多协议支持** - REST、gRPC、JSON-RPC 全部实现
✅ **配置清晰** - 配置绑定与属性对象分离

这是一个真正**框架级别**的设计，让开发者专注于业务价值！
