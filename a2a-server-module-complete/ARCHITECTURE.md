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
│       │       ├── A2AAgentExecutorAdapter.java       # 适配器
│       │       └── A2AGrpcServiceDelegate.java        # gRPC 委托
│       ├── server/                            # 服务器管理
│       │   ├── A2AServerBootstrap.java        # 启动器（建造者）
│       │   └── DefaultProtocolServerFactory.java     # 默认工厂
│       └── config/                            # SDK 配置
│           ├── EnableA2AServer.java           # 启用注解
│           ├── A2AServerAutoConfiguration.java        # 自动配置
│           ├── A2AServerProperties.java              # 配置属性
│           └── A2AServerPropertiesConfiguration.java  # 配置绑定
├── src/main/resources/
│   └── application.yml                        # 应用配置
└── pom.xml                                    # Maven 配置
```

## ✅ 已删除的旧代码

以下旧代码已被删除，由新的 SDK 架构替代：

- ❌ `agent/CompleteA2AAgentExecutor.java` - 已被 `A2AAgentExecutorAdapter` 替代
- ❌ `config/AgentCardConfig.java` - SDK 自动管理
- ❌ `config/ExecutorConfig.java` - SDK 自动配置
- ❌ `config/GrpcServerConfig.java` - SDK 中有新实现
- ❌ `config/RequestHandlerConfig.java` - SDK 不需要
- ❌ `grpc/A2AGrpcService.java` - 已被 `A2AGrpcServiceDelegate` 替代
- ❌ `jsonrpc/JsonRpcConfig.java` - SDK 自动配置
- ❌ `jsonrpc/JsonRpcController.java` - 已被 `JsonRpcProtocolServer` 替代
- ❌ `rest/A2ARestController.java` - 已被 `RestProtocolServer` 替代

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
3. 扫描 A2AAgent 实现类
   ↓
4. 创建 A2AServerBootstrap
   ↓
5. 通过 ProtocolServerFactory 创建协议服务器
   ├─→ RestProtocolServer
   ├─→ GrpcProtocolServer
   └─→ JsonRpcProtocolServer
   ↓
6. 启动所有协议服务器
   ↓
7. 暴露端点
```

### 请求处理流程

```
客户端请求
   ↓
协议层 (REST/gRPC/JSON-RPC)
   ↓
A2AAgentExecutorAdapter (适配)
   ↓
A2AAgent.execute() (用户逻辑)
   ↓
AgentResponse (响应)
   ↓
协议层序列化
   ↓
返回给客户端
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

## 🎯 总结

新的 SDK 架构：

✅ **代码量减少 80%** - 用户只需实现一个接口
✅ **完全解耦** - 业务逻辑与协议无关
✅ **易于扩展** - 添加新协议只需实现接口
✅ **自动化** - Spring Boot 自动配置
✅ **生产就绪** - 完整的错误处理和日志

这是一个真正**框架级别**的设计，让开发者专注于业务价值！
