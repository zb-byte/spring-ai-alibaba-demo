# A2A gRPC Client Demo

基于 A2A Java SDK v0.3.3.Final 的最小可运行 gRPC 客户端演示项目。

## 项目概述

这是一个使用 A2A Java SDK 构建的 gRPC 客户端演示，用于与 A2A gRPC 服务器进行通信。项目提供了 Web 界面和 REST API 来测试 A2A 协议的各种功能。

## 技术栈

- **Spring Boot 3.x** - 应用框架
- **A2A Java SDK v0.3.3.Final** - A2A 协议客户端
- **gRPC** - 通信协议
- **Maven** - 构建工具
- **Java 21** - 运行环境

## 项目结构

```
a2a-client-module-grpc/
├── src/main/java/com/example/a2aclient/
│   ├── A2aClientModuleApplication.java    # Spring Boot 主类
│   ├── SimpleA2AClientDemo.java           # 命令行演示
│   ├── A2AClientService.java              # A2A 客户端服务
│   └── controller/
│       ├── ClientDemoController.java      # REST API 控制器
│       └── WebController.java             # Web 界面控制器
├── src/main/resources/
│   └── application.yml                    # 配置文件
├── pom.xml                               # Maven 配置
├── start-client.sh                       # 启动脚本
└── README.md                            # 项目说明
```

## 功能特性

### ✅ 已实现功能

1. **Maven 依赖配置** - A2A SDK v0.3.3.Final 完整依赖
2. **Spring Boot 集成** - 完整的 Spring Boot 应用框架
3. **服务器连接测试** - HTTP 和 gRPC 端口连通性测试
4. **Agent Card 获取** - 支持 HTTP 和 A2A SDK 两种方式
5. **A2A 客户端服务框架** - 异步消息处理框架
6. **消息发送/接收** - HTTP 模拟和 A2A SDK 框架实现
7. **Web 界面** - 完整的测试界面

### ⏳ 待开发功能

1. **完整 gRPC 客户端实现** - 需要深入研究 A2A SDK API
2. **流式响应处理** - 支持流式消息传输
3. **错误处理和重试机制** - 生产级错误处理
4. **配置管理** - 更灵活的配置选项

## 快速开始

### 1. 环境要求

- Java 21+
- Maven 3.8+
- 运行中的 A2A gRPC Server

### 2. 启动服务器

首先确保 A2A Server 正在运行：

```bash
cd ../a2a-server-module-grpc
./start-server.sh
```

### 3. 启动客户端

```bash
# 使用启动脚本
./start-client.sh

# 或者直接使用 Maven
mvn spring-boot:run
```

### 4. 访问应用

- **Web 界面**: http://localhost:7001/
- **API 文档**: 见下方 API 端点说明

## 配置说明

### application.yml

```yaml
server:
  port: 7001  # 客户端 Web 端口

a2a:
  server:
    host: localhost      # A2A 服务器地址
    port: 7002          # A2A 服务器 HTTP 端口
    grpc-port: 9090     # A2A 服务器 gRPC 端口
    agent-card-url: "http://localhost:7002/.well-known/agent-card.json"
```

## API 端点

### 连接测试
- **GET** `/api/test-connection` - 测试服务器连接
- **GET** `/api/test-grpc` - 测试 gRPC 连接

### Agent Card
- **GET** `/api/agent-card` - 通过 HTTP 获取 Agent Card
- **GET** `/api/agent-card-a2a` - 通过 A2A SDK 获取 Agent Card

### 消息发送
- **POST** `/api/send-message` - 通过 HTTP 发送消息
- **GET** `/api/send-message-a2a?message=xxx` - 通过 A2A SDK 发送消息

## 使用示例

### 1. Web 界面测试

1. 访问 http://localhost:7001/
2. 点击 "测试连接" 验证服务器状态
3. 点击 "获取 Agent Card" 查看 Agent 信息
4. 输入消息并测试发送功能

### 2. API 调用示例

```bash
# 测试连接
curl http://localhost:7001/api/test-connection

# 获取 Agent Card
curl http://localhost:7001/api/agent-card

# 发送消息
curl -X POST http://localhost:7001/api/send-message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, A2A!"}'

# 通过 A2A SDK 发送消息
curl "http://localhost:7001/api/send-message-a2a?message=Hello%20A2A"
```

## 开发说明

### A2A SDK 集成

由于 A2A Java SDK v0.3.3.Final 的 API 复杂性，当前实现提供了完整的框架代码，但实际的 gRPC 通信部分使用模拟实现。

主要组件：

1. **A2AClientService** - A2A 客户端服务封装
2. **SimpleA2AClientDemo** - 命令行演示和连接测试
3. **ClientDemoController** - REST API 控制器

### 扩展开发

要实现完整的 A2A gRPC 客户端，需要：

1. 研究 A2A SDK v0.3.3.Final 的实际 API
2. 在 `A2AClientService` 中实现真正的 SDK 调用
3. 处理 gRPC 连接和消息传输
4. 实现错误处理和重试机制

## 故障排除

### 常见问题

1. **连接失败**
   - 确保 A2A Server 正在运行
   - 检查端口配置是否正确

2. **编译错误**
   - 检查 Java 版本 (需要 21+)
   - 确保 Maven 依赖下载完成

3. **gRPC 连接问题**
   - 检查防火墙设置
   - 确认 gRPC 端口 (9090) 可访问

### 日志查看

应用启动后会输出详细的连接测试信息，包括：
- 服务器连接状态
- Agent Card 获取结果
- gRPC 端口连通性

## 参考资源

- [A2A Protocol Specification](https://a2a-protocol.org/)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java)
- [Maven Repository](https://mvnrepository.com/artifact/io.github.a2asdk)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## 许可证

本项目仅用于演示目的，请遵循相关开源协议。