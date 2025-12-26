# 测试指南

服务启动后，可以通过以下方式测试三种传输协议。

## 快速测试

### 方法一：使用测试脚本（推荐）

```bash
# 给脚本添加执行权限
chmod +x test-api.sh

# 运行测试脚本
./test-api.sh
```

### 方法二：使用 curl 命令

#### 1. 查看当前传输协议类型

```bash
curl http://localhost:8080/demo/transport/type
```

**预期响应：**
```json
{
  "currentTransportType": "JSONRPC",
  "supportedTypes": ["JSONRPC", "GRPC", "HTTP+JSON"],
  "agentCard": {
    "name": "demo-react-agent",
    "description": "一个通过 A2A 暴露的 ReactAgent 示例",
    "url": "http://127.0.0.1:8080/a2a",
    "capabilities": {
      "streaming": true
    }
  }
}
```

#### 2. 测试本地 Agent 调用（不通过 A2A）

```bash
curl -X POST http://localhost:8080/demo/transport/local \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}'
```

#### 3. 测试 A2A 协议调用（使用当前配置的协议）

```bash
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}'
```

**预期响应：**
```json
{
  "transport": "a2a",
  "protocol": "JSONRPC",
  "input": "你好，请用一句话介绍自己",
  "response": "你好！我是一个智能助手...",
  "agentCard": {
    "name": "demo-react-agent",
    "url": "http://127.0.0.1:8080/a2a"
  }
}
```

#### 4. 获取 Agent Card 信息

```bash
curl http://localhost:8080/demo/transport/agent-card
```

#### 5. 获取 A2A 标准 Agent Card

```bash
curl http://localhost:8080/.well-known/agent.json
```

### 方法三：使用 HTTP 客户端工具

#### Postman/Insomnia

1. **GET 请求** - 查看协议类型
   - URL: `http://localhost:8080/demo/transport/type`
   - Method: GET

2. **POST 请求** - 测试 A2A 调用
   - URL: `http://localhost:8080/demo/transport/a2a`
   - Method: POST
   - Headers: `Content-Type: application/json`
   - Body (JSON):
     ```json
     {
       "message": "你好，请介绍一下 Spring AI Alibaba"
     }
     ```

#### 使用 httpie

```bash
# 安装 httpie (如果还没有)
# macOS: brew install httpie
# Linux: apt-get install httpie

# 查看协议类型
http GET localhost:8080/demo/transport/type

# 测试 A2A 调用
http POST localhost:8080/demo/transport/a2a message="你好，请介绍一下 Spring AI Alibaba"
```

## 测试不同协议

### 测试 JSON-RPC 协议

如果使用 `application-jsonrpc.yml` 配置启动：

```bash
# 启动时指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=jsonrpc

# 然后测试
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "测试 JSON-RPC 协议"}'
```

### 测试 gRPC 协议

如果使用 `application-grpc.yml` 配置启动：

```bash
# 启动时指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=grpc

# 然后测试
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "测试 gRPC 协议"}'
```

### 测试 REST 协议

如果使用 `application-rest.yml` 配置启动：

```bash
# 启动时指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=rest

# 然后测试
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "测试 REST 协议"}'
```

## 流式通信测试

如果需要测试流式通信，可以使用 Server-Sent Events (SSE)：

```bash
curl -N -H "Accept: text/event-stream" \
  -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "请详细介绍一下 Spring AI Alibaba"}'
```

## 常见问题

### 1. 端口被占用

如果 8080 端口被占用，可以修改 `application.yml` 中的 `server.port`：

```yaml
server:
  port: 8081
```

### 2. API Key 未配置

确保设置了 `DEFAULT_LLM_API_KEY` 环境变量，或在 `application.yml` 中配置。

### 3. 查看日志

如果遇到问题，可以查看应用日志：

```bash
# 如果使用 Maven 启动，日志会直接输出到控制台
# 日志级别在 application.yml 中配置为 DEBUG
```

### 4. 测试 A2A 标准端点

A2A 协议的标准端点：

- **Agent Card**: `GET http://localhost:8080/.well-known/agent.json`
- **消息端点**: `POST http://localhost:8080/a2a` (JSON-RPC)

## 使用 jq 美化输出

如果安装了 `jq`，可以在 curl 命令后添加 `| jq .` 来美化 JSON 输出：

```bash
curl -s http://localhost:8080/demo/transport/type | jq .
```

## 完整测试流程

```bash
# 1. 检查服务是否启动
curl http://localhost:8080/demo/transport/type

# 2. 测试本地调用
curl -X POST http://localhost:8080/demo/transport/local \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 3. 测试 A2A 调用
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 4. 查看 Agent Card
curl http://localhost:8080/.well-known/agent.json
```

