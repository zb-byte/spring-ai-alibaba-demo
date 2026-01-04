### 启动步骤

#### 方式一：使用启动脚本（推荐）

1. **启动 Reviewer Service**（必须先启动）
   ```bash
   cd spring-ai-alibaba-demo
   chmod +x scripts/start-reviewer.sh
   ./scripts/start-reviewer.sh
   ```

   等待看到以下日志表示启动成功：
   ```
   Started ReviewerApplication in X.XXX seconds
   ```

2. **启动 Writer Service**（新开一个终端）
   ```bash
   cd spring-ai-alibaba-demo
   chmod +x scripts/start-writer.sh
   ./scripts/start-writer.sh
   ```

3. **（可选）启动 Demo Client**
   ```bash
   cd demo-client
   mvn spring-boot:run
   ```

#### 方式二：手动启动

1. **启动 Reviewer Service**
   ```bash
   cd reviewer-service
   mvn spring-boot:run
   ```
   服务将在 `http://localhost:8081` 启动

2. **启动 Writer Service**（新开终端）
   ```bash
   cd writer-service
   mvn spring-boot:run
   ```
   服务将在 `http://localhost:8080` 启动

3. **（可选）启动 Demo Client**（新开终端）
   ```bash
   cd demo-client
   mvn spring-boot:run
   ```
   服务将在 `http://localhost:8082` 启动

## 🧪 测试示例

### 1. 健康检查

```bash
# Writer Service
curl http://localhost:8080/api/health

# Reviewer Service (通过 A2A)
curl http://localhost:8081/.well-known/agent.json
```

### 2. 使用写作和评审服务（完整流程）

`/api/planner/invoke` 接口提供完整的写作+评审流程：
1. 首先使用 Writer Agent 根据主题生成文章
2. 然后通过 A2A 协议调用 Reviewer Agent 对文章进行评审和修改

**接口说明**：
- **必需参数**：`topic` - 文章主题或描述
- 自动执行：写作 → 评审 → 返回最终文章

```bash
# 使用 topic 参数（必需）
curl -X POST http://localhost:8080/api/planner/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "请写一篇关于人工智能的文章，大约200字"
  }'

# 另一个示例
curl -X POST http://localhost:8080/api/planner/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "春天的景色"
  }'
```

**响应示例**：
```json
{
  "result": "人工智能（AI）是21世纪最重要的技术之一...（评审后的文章）"
}
```

**工作流程**：
1. Writer Agent 根据用户提供的主题生成原始文章
2. 如果 Reviewer Service 可用，通过 A2A 协议调用 Reviewer Agent 进行评审
3. 如果 Reviewer Service 不可用，返回原始文章
4. 返回最终的文章内容（评审后的文章或原始文章）

## 📚 核心概念

### 1. ReactAgent

基于 **ReAct (Reasoning + Acting)** 模式的智能 Agent，支持：
- 推理和行动循环
- 工具调用
- 结构化输入/输出

### 2. A2A 协议

**Agent-to-Agent (A2A)** 是 Google 提出的 Agent 间通信协议，支持：
- 服务发现（通过 `/.well-known/agent.json`）
- 标准化的消息传递
- 多种传输协议（JSON-RPC、gRPC、REST）

### 3. WriteAndReviewService（写作和评审服务）

`WriteAndReviewService` 封装了完整的写作和评审流程：
- **Writer Agent** - 根据主题生成文章
- **A2A 协议调用** - 通过 A2A 协议调用远程 Reviewer Agent
- **自动流程** - 自动执行写作 → 评审 → 返回的完整流程

**注意**：当前实现使用固定的服务流程，而不是 Planner Agent 的智能调度。Planner Agent 虽然已配置，但当前接口直接调用 `WriteAndReviewService` 来执行固定的写作+评审流程。

## 🔧 配置说明

### Writer Service 配置

`writer-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  ai:
    alibaba:
      a2a:
        server:
          enabled: false  # Writer Service 不暴露 A2A 服务
        nacos:
          discovery:
            enabled: false
          registry:
            enabled: false

reviewer:
  agent:
    url: http://127.0.0.1:8081  # Reviewer Service 地址
```

### Reviewer Service 配置

`reviewer-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  ai:
    alibaba:
      a2a:
        server:
          type: JSONRPC
          address: 127.0.0.1
          port: 8081
          message-url: /a2a
          card:
            name: reviewer-agent
            description: 一个专业的文章评审 Agent
            url: http://127.0.0.1:8081/a2a
```

## 🐛 常见问题

### 1. 启动失败：无法连接到 Reviewer Service

**错误信息**：
```
java.net.ConnectException: Connection refused
```

**解决方案**：
- 确保 Reviewer Service 已先启动
- 检查端口 8081 是否被占用
- 确认 `reviewer.agent.url` 配置正确

### 2. Nacos 相关错误

**错误信息**：
```
NacosRuntimeException: Request Nacos server version is too low
```

**解决方案**：
- 已在配置中禁用 Nacos，如果仍有问题，检查 `application.yml` 中的配置

### 3. LLM API 调用失败

**错误信息**：
```
Failed to call LLM API
```

**解决方案**：
- 检查环境变量 `DEFAULT_LLM_API_KEY` 是否正确
- 确认 `DEFAULT_LLM_BASE_URL` 可访问
- 验证 API Key 是否有足够的权限

### 4. Agent Card 获取失败

**错误信息**：
```
Failed to obtain agent card
```

**解决方案**：
- 确保 Reviewer Service 已启动
- 访问 `http://localhost:8081/.well-known/agent.json` 验证服务是否正常
- 检查网络连接


## 💡 提示

- 启动顺序很重要：**必须先启动 Reviewer Service，再启动 Writer Service**
- `/api/planner/invoke` 接口会自动执行完整的写作+评审流程
- 接口**必需** `topic` 参数，用于指定文章主题或描述
- 如果 Reviewer Service 不可用，接口仍会返回 Writer Agent 生成的文章
- 所有 Agent 共享相同的 LLM 配置，可通过环境变量统一管理
- 建议使用 `jq` 工具美化 JSON 输出：`curl ... | jq .`

---

**祝使用愉快！如有问题，请查看日志或提交 Issue。**
