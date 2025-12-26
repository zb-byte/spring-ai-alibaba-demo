# Spring AI Alibaba 三种传输协议演示

本项目演示了 Spring AI Alibaba 支持的三种 A2A 传输协议的使用方法。

## 支持的传输协议

Spring AI Alibaba 支持以下三种传输协议：

1. **JSON-RPC 2.0** - 基于 JSON 的远程过程调用协议
2. **gRPC** - Google 开发的高性能 RPC 框架
3. **HTTP+JSON/REST** - 基于 HTTP 的 RESTful API

## 项目结构

```
spring-ai-alibaba-demo/
├── src/main/resources/
│   ├── application.yml                    # 默认配置
│   ├── application-jsonrpc.yml           # JSON-RPC 协议配置
│   ├── application-grpc.yml              # gRPC 协议配置
│   └── application-rest.yml              # REST 协议配置
├── src/main/java/com/example/aidemo/
│   └── demo/
│       ├── TransportProtocolDemoController.java  # 演示控制器
│       └── A2aClientDemo.java                    # 客户端使用参考
└── TRANSPORT_PROTOCOLS_DEMO.md           # 本文档
```

## 配置方式

### 1. JSON-RPC 2.0 协议

使用 `application-jsonrpc.yml` 配置文件：

```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: JSONRPC  # 指定使用 JSON-RPC 协议
          message-url: /a2a
          card:
            name: demo-react-agent-jsonrpc
```

**启动方式：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=jsonrpc
# 或者
java -jar target/spring-ai-alibaba-demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=jsonrpc
```

### 2. gRPC 协议

使用 `application-grpc.yml` 配置文件：

```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: GRPC  # 指定使用 gRPC 协议
          message-url: /a2a
          card:
            name: demo-react-agent-grpc
```

**启动方式：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=grpc
# 或者
java -jar target/spring-ai-alibaba-demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=grpc
```

### 3. HTTP+JSON/REST 协议

使用 `application-rest.yml` 配置文件：

```yaml
spring:
  ai:
    alibaba:
      a2a:
        server:
          type: HTTP+JSON  # 指定使用 HTTP+JSON/REST 协议
          message-url: /a2a
          card:
            name: demo-react-agent-rest
```

**启动方式：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=rest
# 或者
java -jar target/spring-ai-alibaba-demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=rest
```

## API 端点

启动应用后，可以使用以下 API 端点：

### 1. 获取当前传输协议类型

```bash
GET http://localhost:8080/demo/transport/type
```

**响应示例：**
```json
{
  "currentTransportType": "JSONRPC",
  "supportedTypes": ["JSONRPC", "GRPC", "HTTP+JSON"],
  "agentCard": {
    "name": "demo-react-agent-jsonrpc",
    "description": "一个通过 JSON-RPC 2.0 协议暴露的 ReactAgent 示例",
    "url": "http://127.0.0.1:8080/a2a",
    "capabilities": {
      "streaming": true
    }
  }
}
```

### 2. 本地 Agent 调用（不通过 A2A）

```bash
POST http://localhost:8080/demo/transport/local
Content-Type: application/json

{
  "message": "你好，请介绍一下自己"
}
```

**响应示例：**
```json
{
  "transport": "local",
  "input": "你好，请介绍一下自己",
  "response": "你好！我是一个智能助手...",
  "protocol": "N/A (本地调用)"
}
```

### 3. 通过 A2A 协议调用

```bash
POST http://localhost:8080/demo/transport/a2a
Content-Type: application/json

{
  "message": "你好，请介绍一下自己"
}
```

**响应示例：**
```json
{
  "transport": "a2a",
  "protocol": "JSONRPC",
  "input": "你好，请介绍一下自己",
  "response": "你好！我是一个智能助手...",
  "agentCard": {
    "name": "demo-react-agent-jsonrpc",
    "url": "http://127.0.0.1:8080/a2a"
  }
}
```

### 4. 获取 Agent Card

```bash
GET http://localhost:8080/demo/transport/agent-card
```

## A2A 协议端点

根据配置的协议类型，A2A 服务会暴露以下端点：

### JSON-RPC 2.0

- **Agent Card**: `GET http://localhost:8080/.well-known/agent.json`
- **消息端点**: `POST http://localhost:8080/a2a`

**请求示例：**
```json
{
  "jsonrpc": "2.0",
  "method": "a2a.message.send",
  "params": {
    "message": {
      "role": "user",
      "content": "Hello"
    }
  },
  "id": 1
}
```

### gRPC

- **Agent Card**: `GET http://localhost:8080/.well-known/agent.json`
- **消息端点**: gRPC 服务端点（默认端口 8080）

### HTTP+JSON/REST

- **Agent Card**: `GET http://localhost:8080/.well-known/agent.json`
- **发送消息**: `POST http://localhost:8080/v1/message:send`
- **流式消息**: `POST http://localhost:8080/v1/message:stream`

**请求示例：**
```json
{
  "message": {
    "role": "user",
    "content": "Hello"
  }
}
```

## 流式通信

所有三种协议都支持流式通信：

- **JSON-RPC**: 使用 Server-Sent Events (SSE)
- **gRPC**: 使用 gRPC 流式 RPC
- **REST**: 使用 Server-Sent Events (SSE)

流式请求使用 `a2a.message.sendStreaming` 方法（JSON-RPC）或 `/v1/message:stream` 端点（REST）。

## 客户端使用

要创建 A2A 客户端，需要添加 A2A Java SDK 依赖：

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

详细客户端使用示例请参考 `A2aClientDemo.java` 文件中的注释。

## 测试步骤

1. **启动服务（JSON-RPC 模式）**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=jsonrpc
   ```

2. **测试本地调用**
   ```bash
   curl -X POST http://localhost:8080/demo/transport/local \
     -H "Content-Type: application/json" \
     -d '{"message": "你好"}'
   ```

3. **测试 A2A 调用**
   ```bash
   curl -X POST http://localhost:8080/demo/transport/a2a \
     -H "Content-Type: application/json" \
     -d '{"message": "你好"}'
   ```

4. **查看当前协议类型**
   ```bash
   curl http://localhost:8080/demo/transport/type
   ```

5. **获取 Agent Card**
   ```bash
   curl http://localhost:8080/.well-known/agent.json
   ```

## 切换协议

要切换不同的传输协议，只需：

1. 修改 `application.yml` 中的 `spring.ai.alibaba.a2a.server.type` 配置
2. 或者使用不同的 profile 启动应用

**注意：** 当前实现中，JSON-RPC 是默认协议，也是唯一完全实现的协议。gRPC 和 REST 协议的支持可能需要额外的依赖和配置。

## 参考文档

- [Spring AI Alibaba 官方文档](https://java2ai.com)
- [A2A 协议规范](https://github.com/a2aproject/a2a-spec)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java)

