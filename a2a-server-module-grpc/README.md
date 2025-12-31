# A2A gRPC Server Demo

基于 A2A Java SDK v0.3.3.Final 和 Spring AI 构建的 A2A gRPC Server Demo。

## ✅ 已完成功能

- **Maven 依赖配置**: 正确配置了 A2A SDK 0.3.3.Final 版本的所有必需依赖
- **Spring Boot 集成**: 成功集成 Spring Boot 框架
- **Spring AI 集成**: 集成 Spring AI OpenAI 支持，可使用 ChatGPT 进行对话
- **Agent Card 端点**: 提供标准的 `/.well-known/agent-card.json` 端点
- **gRPC 服务实现**: 完整实现 A2A gRPC 服务端点
- **Agent 执行器**: 实现 Spring AI 驱动的 Agent 执行器

## 🏗️ 项目结构

```
src/main/java/com/example/a2aserver/
├── A2aServerModuleApplication.java           # 主应用程序类
├── agent/
│   └── SpringAIAgentExecutor.java            # Spring AI Agent 执行器
├── config/
│   └── AgentCardConfig.java                  # Agent Card 配置
├── controller/
│   └── AgentCardController.java              # Agent Card HTTP 端点
├── events/
│   └── SimpleQueueManager.java               # 事件队列管理器
└── grpc/
    ├── A2AGrpcService.java                   # A2A gRPC 服务实现
    └── GrpcServerConfig.java                 # gRPC 服务器配置
```

## 📦 Maven 依赖

### 核心 A2A SDK 依赖 (v0.3.3.Final)
- `a2a-java-sdk-spec`: A2A 协议规范定义
- `a2a-java-sdk-spec-grpc`: gRPC 协议绑定
- `a2a-java-sdk-common`: 通用组件
- `a2a-java-sdk-server-common`: 服务器通用组件
- `a2a-java-sdk-transport-grpc`: gRPC 传输层实现

### Spring AI 依赖
- `spring-ai-openai-spring-boot-starter`: Spring AI OpenAI 集成

### 其他依赖
- Spring Boot Web Starter
- gRPC 相关依赖 (netty, protobuf, stub, services)

## 🚀 启动方式

### 1. 配置环境变量
```bash
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://api.openai.com  # 可选，默认为 OpenAI 官方 API
export OPENAI_MODEL=gpt-3.5-turbo              # 可选，默认为 gpt-3.5-turbo
```

### 2. 使用 Maven 启动
```bash
cd spring-ai-alibaba-demo/a2a-server-module-grpc
mvn spring-boot:run
```

### 3. 使用 JAR 包启动
```bash
mvn clean package -DskipTests
java -jar target/a2a-server-module-grpc-0.0.1-SNAPSHOT.jar
```

## 🔍 验证方式

### 1. 检查服务启动状态
启动成功后，应该看到以下日志：
```
A2AGrpcService initialized
gRPC Server started on port 9090
```

### 2. 获取 Agent Card
```bash
curl http://localhost:7002/.well-known/agent-card.json
```

### 3. 使用 gRPC 客户端测试
可以使用 `a2a-client-module-grpc` 模块进行测试。

## 🔧 配置说明

### application.yml
```yaml
server:
  port: 7002                    # HTTP 端口

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-3.5-turbo}

grpc:
  server:
    port: 9090                  # gRPC 端口

agent:
  name: Spring AI Echo Agent
  description: A Spring AI powered A2A Agent
```

## 📡 gRPC 服务端点

| 方法 | 描述 |
|------|------|
| `sendMessage` | 发送消息并等待完成 |
| `sendStreamingMessage` | 发送消息并流式接收响应 |
| `getTask` | 获取任务状态 |
| `cancelTask` | 取消任务 |
| `taskSubscription` | 订阅任务更新 |
| `getAgentCard` | 获取 Agent Card |

## 🛠️ 技术细节

### A2A SDK v0.3.3.Final API 特点
- 使用 `new Builder()` 构造器模式而非 `builder()` 静态方法
- `Message`, `Task` 等是 class 而非 record
- 使用 getter 方法如 `getTaskId()`, `getContextId()`
- `AgentCard` 使用 `url` 字段而非 `supportedInterfaces`
- `AgentExecutor` 接口声明 `throws JSONRPCError`

### Spring AI 集成
- 使用 `ChatClient` 进行 AI 对话
- 支持自定义系统提示词
- 当 AI 调用失败时自动降级为 Echo 模式

## 📚 参考资源

- **A2A Protocol**: https://a2a-protocol.org/
- **A2A Java SDK**: https://github.com/a2aproject/a2a-java
- **Spring AI**: https://docs.spring.io/spring-ai/reference/
- **gRPC Java**: https://grpc.io/docs/languages/java/

## 📝 开发日志

- ✅ 2025-12-31: 完成基础项目框架搭建
- ✅ 2025-12-31: 配置 Maven 依赖 (A2A SDK 0.3.3.Final)
- ✅ 2025-12-31: 实现 Agent Card 端点
- ✅ 2025-12-31: 实现 gRPC 服务端点
- ✅ 2025-12-31: 集成 Spring AI
- ✅ 2025-12-31: 实现 SpringAIAgentExecutor
