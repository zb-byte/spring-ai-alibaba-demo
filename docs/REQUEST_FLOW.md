# 请求流程说明

## 请求流程：`POST /demo/transport/a2a`

### 1. 请求入口
```
客户端 → POST /demo/transport/a2a
       Body: {"message": "你好"}
```

### 2. Controller 处理
```
TransportProtocolDemoController.chatViaA2a()
  ↓
1. 提取 message 参数
2. 调用 remoteAgent.invoke(input)
   - remoteAgent 是 A2aRemoteAgent 类型
   - 配置的 AgentCard URL: http://127.0.0.1:8080/a2a
```

### 3. A2aRemoteAgent 处理
```
A2aRemoteAgent.invoke(input)
  ↓
1. 构建 A2A 协议请求
2. 通过 HTTP 调用远程 A2A 服务
   - URL: http://127.0.0.1:8080/a2a
   - 协议: JSON-RPC 2.0 (根据配置)
   - 请求格式: JSON-RPC 2.0 格式
  ↓
3. 等待远程服务响应
4. 解析响应并构建 OverAllState
```

### 4. 远程 A2A 服务处理（实际上是同一个应用）
```
A2A Server (JSON-RPC Handler)
  ↓
1. 接收 JSON-RPC 请求
2. 解析请求，提取 message
3. 调用本地 ReactAgent (demoReactAgent)
   - 这是配置在 LlmConfiguration 中的本地 Agent
  ↓
4. ReactAgent 处理
   - 调用 LLM (OpenAiChatModel)
   - 获取 LLM 响应
   - 构建 AssistantMessage
  ↓
5. 返回 JSON-RPC 响应
   - 包含 Task 或 Message 结果
```

### 5. A2aRemoteAgent 解析响应
```
A2aRemoteAgent 收到响应后：
  ↓
1. 解析 JSON-RPC 响应
2. 提取 result 字段
3. 根据 result 的类型构建 OverAllState
   - 可能包含 Task、Message 或 TaskStatusUpdateEvent
4. 返回 Optional<OverAllState>
```

### 6. Controller 提取响应
```
extractAssistantReply(state)
  ↓
1. 从 OverAllState 中查找 "messages" key
2. 提取 List<Message>
3. 过滤出 AssistantMessage
4. 获取最后一个 AssistantMessage 的文本
5. 返回响应文本
```

## 为什么响应为空？

可能的原因：

1. **OverAllState 中没有 "messages" key**
   - A2aRemoteAgent 可能使用不同的 outputKey
   - 响应可能存储在其他的 key 中

2. **响应格式不匹配**
   - A2A 服务返回的可能是 Task 对象，而不是直接的 Message
   - 需要从 Task 中提取 result

3. **异步处理未完成**
   - A2A 调用可能是异步的
   - 需要等待 Task 完成才能获取结果

## 调试方法

使用调试端点查看实际返回的数据：

```bash
curl -X POST http://localhost:8080/demo/transport/a2a/debug \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

这会显示：
- OverAllState 中所有的 key
- 每个 key 的数据类型
- 提取的响应文本
- 如果响应为空，可以看到实际的数据结构

## 解决方案

如果响应为空，可能需要：

1. **检查 A2aRemoteAgent 的 outputKey 配置**
2. **修改 extractAssistantReply 方法**，支持从 Task 中提取结果
3. **使用流式调用**，通过 `/demo/streaming/a2a` 端点

