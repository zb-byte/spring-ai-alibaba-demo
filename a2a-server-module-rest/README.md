# A2A REST Server Demo (Spring AI)

基于 A2A Java SDK v0.3.3.Final 构建的 REST Server Demo，集成 Spring AI 连接大模型。

## 依赖说明

```xml
<properties>
    <io.a2a.sdk.version>0.3.3.Final</io.a2a.sdk.version>
    <spring-ai.version>1.0.0-M4</spring-ai.version>
</properties>

<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring AI - OpenAI Compatible -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>${spring-ai.version}</version>
    </dependency>

    <!-- A2A SDK Core -->
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-spec</artifactId>
        <version>${io.a2a.sdk.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-common</artifactId>
        <version>${io.a2a.sdk.version}</version>
    </dependency>
</dependencies>
```

## 项目结构

```
a2a-server-module-rest/
├── src/main/java/com/example/a2aserver/
│   ├── A2aServerModuleApplication.java  # 启动类
│   ├── config/
│   │   └── AgentCardConfig.java         # AgentCard 配置
│   ├── agent/
│   │   └── SpringAIAgentExecutor.java   # Spring AI Agent 执行器
│   └── handler/
│       └── A2ARestController.java       # REST API 控制器
└── src/main/resources/
    └── application.yml                   # 配置文件
```

## 配置说明

### application.yml

```yaml
server:
  port: 7002

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key}
      base-url: ${OPENAI_BASE_URL:https://ark.cn-beijing.volces.com}
      chat:
        options:
          model: ${OPENAI_MODEL:deepseek-v3-250324}
          temperature: 0.7
```

### 环境变量配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OPENAI_API_KEY` | API 密钥 | - |
| `OPENAI_BASE_URL` | API 端点 | `https://ark.cn-beijing.volces.com` |
| `OPENAI_MODEL` | 模型名称 | `deepseek-v3-250324` |

## 启动方式

```bash
# 设置环境变量
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://ark.cn-beijing.volces.com
export OPENAI_MODEL=deepseek-v3-250324

# 启动服务
cd a2a-server-module-rest
mvn spring-boot:run
```

或者：

```bash
mvn clean package -DskipTests
java -jar target/a2a-server-module-rest-0.0.1-SNAPSHOT.jar
```

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/.well-known/agent-card.json` | 获取 Agent Card |
| POST | `/v1/message:send` | 发送消息 (同步) |
| POST | `/v1/message:stream` | 发送消息 (流式 SSE) |
| GET | `/v1/tasks/{taskId}` | 获取 Task |
| POST | `/v1/tasks/{taskId}:cancel` | 取消 Task |

## 测试

```bash
# 获取 Agent Card
curl http://localhost:7002/.well-known/agent-card.json

# 发送消息 (同步)
curl -X POST http://localhost:7002/v1/message:send \
  -H "Content-Type: application/json" \
  -d '{"message":{"messageId":"test-1","role":"user","contextId":"ctx-1","parts":[{"text":"你好，请介绍一下自己"}]}}'

# 流式消息
curl -X POST http://localhost:7002/v1/message:stream \
  -H "Content-Type: application/json" \
  -d '{"message":{"messageId":"test-2","role":"user","contextId":"ctx-2","parts":[{"text":"请用100字介绍人工智能"}]}}'
```

## 关键代码

### SpringAIAgentExecutor.java

```java
@Component
public class SpringAIAgentExecutor {
    private final ChatClient chatClient;

    public SpringAIAgentExecutor(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个友好的 AI 助手。")
                .build();
    }

    public Message processMessage(MessageSendParams params) {
        String userInput = extractTextFromMessage(params.message());
        String response = chatClient.prompt()
                .user(userInput)
                .call()
                .content();
        
        return new Message.Builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.AGENT)
                .contextId(params.message().getContextId())
                .parts(List.of(new TextPart(response)))
                .build();
    }
}
```
