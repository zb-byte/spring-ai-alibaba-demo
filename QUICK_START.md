# 快速开始 - 三种传输协议演示

## 快速测试

### 1. 使用 JSON-RPC 2.0 协议

```bash
# 启动应用（JSON-RPC 模式）
mvn spring-boot:run -Dspring-boot.run.profiles=jsonrpc

# 在另一个终端测试
# 查看当前协议类型
curl http://localhost:8080/demo/transport/type

# 测试本地调用
curl -X POST http://localhost:8080/demo/transport/local \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}'

# 测试 A2A 调用
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请用一句话介绍自己"}'

# 获取 Agent Card
curl http://localhost:8080/.well-known/agent.json
```

### 2. 使用 gRPC 协议

```bash
# 启动应用（gRPC 模式）
mvn spring-boot:run -Dspring-boot.run.profiles=grpc

# 测试（同上）
curl http://localhost:8080/demo/transport/type
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

### 3. 使用 HTTP+JSON/REST 协议

```bash
# 启动应用（REST 模式）
mvn spring-boot:run -Dspring-boot.run.profiles=rest

# 测试（同上）
curl http://localhost:8080/demo/transport/type
curl -X POST http://localhost:8080/demo/transport/a2a \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

## 预期响应

### GET /demo/transport/type

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

### POST /demo/transport/a2a

```json
{
  "transport": "a2a",
  "protocol": "JSONRPC",
  "input": "你好，请用一句话介绍自己",
  "response": "你好！我是一个智能助手...",
  "agentCard": {
    "name": "demo-react-agent-jsonrpc",
    "url": "http://127.0.0.1:8080/a2a"
  }
}
```

## 注意事项

1. **环境变量**: 确保设置了 `DEFAULT_LLM_API_KEY` 环境变量，或者修改 `application.yml` 中的配置
2. **端口**: 默认端口是 8080，如果被占用请修改 `server.port`
3. **协议支持**: 当前项目中，JSON-RPC 是默认且完全支持的协议。gRPC 和 REST 可能需要额外的依赖配置

## 更多信息

详细文档请参考 [TRANSPORT_PROTOCOLS_DEMO.md](./TRANSPORT_PROTOCOLS_DEMO.md)

