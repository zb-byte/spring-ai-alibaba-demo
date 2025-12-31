# A2A gRPC Server Demo

基于 A2A Java SDK v0.3.3.Final 构建的最小可运行 gRPC Server Demo 框架。

## ✅ 已完成功能

- **Maven 依赖配置**: 正确配置了 A2A SDK 0.3.3.Final 版本的所有必需依赖
- **Spring Boot 集成**: 成功集成 Spring Boot 框架
- **Agent Card 端点**: 提供标准的 `/.well-known/agent-card.json` 端点
- **Web 界面**: 提供友好的 Web 界面展示 Agent 状态
- **项目结构**: 清晰的项目结构，便于后续开发

## 🏗️ 项目结构

```
src/main/java/com/example/a2aserver/
├── A2aServerModuleApplication.java     # 主应用程序类
├── SimpleA2ADemo.java                  # Demo 启动器和信息展示
└── controller/
    └── AgentCardController.java        # Agent Card HTTP 端点和 Web 界面
```

## 📦 Maven 依赖

### 核心 A2A SDK 依赖 (v0.3.3.Final)
- `a2a-java-sdk-spec`: A2A 协议规范定义
- `a2a-java-sdk-spec-grpc`: gRPC 协议绑定
- `a2a-java-sdk-common`: 通用组件
- `a2a-java-sdk-server-common`: 服务器通用组件
- `a2a-java-sdk-transport-grpc`: gRPC 传输层实现

### 其他依赖
- Spring Boot Web Starter
- gRPC 相关依赖 (netty, protobuf, stub, services)
- Jakarta Annotations API

## 🚀 启动方式

### 1. 使用 Maven 启动
```bash
cd spring-ai-alibaba-demo/a2a-server-module-grpc
mvn spring-boot:run
```

### 2. 使用脚本启动
```bash
./start-server.sh
```

### 3. 使用 JAR 包启动
```bash
mvn clean package
java -jar target/a2a-server-module-grpc-0.0.1-SNAPSHOT.jar
```

## 🔍 验证方式

### 1. 检查服务启动状态
启动成功后，应该看到以下日志：
```
=== A2A gRPC Server Demo ===
基于 A2A Java SDK v0.3.3.Final

✅ Maven 依赖配置完成
✅ Spring Boot 应用启动成功
✅ HTTP 服务器运行在端口: 7002
✅ Agent Card 端点: http://localhost:7002/.well-known/agent-card.json
```

### 2. 访问 Web 界面
```bash
open http://localhost:7002
```

### 3. 获取 Agent Card
```bash
curl http://localhost:7002/.well-known/agent-card.json
```

预期响应：
```json
{
  "name": "Echo Agent",
  "description": "A simple Echo Agent for A2A protocol demo",
  "version": "1.0.0",
  "capabilities": {
    "streaming": true,
    "pushNotifications": false,
    "stateTransitionHistory": false,
    "extensions": []
  },
  "defaultInputModes": ["text"],
  "defaultOutputModes": ["text"],
  "skills": [{
    "id": "echo",
    "name": "Echo Message",
    "description": "Echoes back the received message"
  }],
  "supportedInterfaces": [{
    "protocol": "grpc",
    "url": "grpc://localhost:9090"
  }],
  "protocolVersion": "1.0"
}
```

## 🛠️ 下一步开发建议

### 1. 研究 A2A SDK 0.3.3.Final API
- 查看 `io.a2a.server.agentexecution.AgentExecutor` 接口
- 了解 `io.a2a.server.requesthandlers.RequestHandler` 的使用
- 研究 `io.a2a.transport.grpc.handler.GrpcHandler` 基类

### 2. 实现 Echo Agent 功能
```java
@Component
public class EchoAgentExecutor implements AgentExecutor {
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws A2AError {
        // 实现 Echo 逻辑
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws A2AError {
        // 实现取消逻辑
    }
}
```

### 3. 配置 gRPC 服务器
```java
@Configuration
public class GrpcServerConfig {
    @Bean
    public Server grpcServer(GrpcHandler grpcHandler) {
        return ServerBuilder.forPort(9090)
                .addService(grpcHandler)
                .build();
    }
}
```

### 4. 扩展 Agent 功能
- 添加更多技能 (Weather, Joke, Calculator 等)
- 实现流式响应
- 添加认证授权
- 集成数据库存储

## 📚 参考资源

- **A2A Protocol**: https://a2a-protocol.org/
- **A2A Java SDK**: https://github.com/a2aproject/a2a-java
- **Maven Repository**: https://mvnrepository.com/artifact/io.github.a2asdk
- **gRPC Java**: https://grpc.io/docs/languages/java/
- **Spring Boot**: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/

## 🐛 故障排除

### 常见问题

1. **依赖冲突**
   - 检查 Maven 依赖树：`mvn dependency:tree`
   - 确保使用正确的 A2A SDK 版本 (0.3.3.Final)

2. **端口冲突**
   - HTTP 端口：7002 (可在 application.yml 中修改)
   - gRPC 端口：9090 (待实现时配置)

3. **API 兼容性**
   - A2A SDK 0.3.3.Final 使用 record 类型
   - 构造函数参数与较新版本不同
   - 需要仔细研究实际 API 文档

## 📝 开发日志

- ✅ 2025-12-31: 完成基础项目框架搭建
- ✅ 2025-12-31: 配置 Maven 依赖 (A2A SDK 0.3.3.Final)
- ✅ 2025-12-31: 实现 Agent Card 端点
- ✅ 2025-12-31: 添加 Web 界面
- ⏳ 待完成: gRPC 服务器实现
- ⏳ 待完成: Echo Agent 业务逻辑
- ⏳ 待完成: 客户端测试工具

---

**注意**: 这是一个基础框架，gRPC 服务器和 Agent 业务逻辑需要进一步开发。由于 A2A SDK 0.3.3.Final 版本的 API 复杂性，建议先深入研究 SDK 文档和示例代码。