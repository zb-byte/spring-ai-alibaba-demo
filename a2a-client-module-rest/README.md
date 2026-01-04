# A2A REST Client Demo

基于 A2A Java SDK v0.3.3.Final 构建的最小可运行 REST Client Demo。

## 依赖说明

```xml
<properties>
    <io.a2a.sdk.version>0.3.3.Final</io.a2a.sdk.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- A2A SDK Client -->
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-client</artifactId>
        <version>${io.a2a.sdk.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-client-transport-rest</artifactId>
        <version>${io.a2a.sdk.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-spec</artifactId>
        <version>${io.a2a.sdk.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a2asdk</groupId>
        <artifactId>a2a-java-sdk-http-client</artifactId>
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
a2a-client-module-rest/
├── src/main/java/com/example/a2aclient/
│   ├── A2aClientModuleApplication.java  # 启动类
│   ├── client/
│   │   └── A2ARestClient.java           # A2A REST Client
│   └── controller/
│       └── TestController.java          # 测试 Controller
└── src/main/resources/
    └── application.yml                   # 配置文件
```

## 关键代码

### A2A REST Client (A2ARestClient.java)
```java
// 获取 Agent Card
public AgentCard fetchAgentCard() throws Exception {
    A2ACardResolver resolver = new A2ACardResolver(serverUrl);
    this.agentCard = resolver.getAgentCard();
    this.transport = new RestTransport(agentCard);
    return agentCard;
}

// 发送消息 (同步)
public EventKind sendMessage(String text) throws Exception {
    Message message = new Message.Builder()
            .messageId(UUID.randomUUID().toString())
            .role(Message.Role.USER)
            .contextId(UUID.randomUUID().toString())
            .parts(List.of(new TextPart(text)))
            .build();
    
    MessageSendParams params = new MessageSendParams(message, null, null);
    return transport.sendMessage(params, null);
}

// 发送消息 (流式)
public void sendMessageStreaming(String text, Consumer<StreamingEventKind> eventConsumer) throws Exception {
    // ... 构建 message 和 params
    transport.sendMessageStreaming(params, eventConsumer, errorConsumer, null);
}
```

## 启动方式

**前提：先启动 Server (端口 7002)**

```bash
cd a2a-client-module-rest
mvn spring-boot:run
```

或者：

```bash
mvn clean package -DskipTests
java -jar target/a2a-client-module-rest-0.0.1-SNAPSHOT.jar
```

## 测试端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/test/agent-card` | 获取 Agent Card |
| POST | `/test/send?msg=xxx` | 发送消息 (同步) |
| POST | `/test/stream?msg=xxx` | 发送消息 (流式) |
| GET | `/test/task/{taskId}` | 获取 Task |

## 测试

```bash
# 获取 Agent Card
curl http://localhost:7001/test/agent-card

# 发送消息
curl -X POST "http://localhost:7001/test/send?msg=Hello"

# 流式消息
curl -X POST "http://localhost:7001/test/stream?msg=Hello%20Stream"
```

## 配置

在 `application.yml` 中配置 Server 地址：

```yaml
a2a:
  server:
    url: http://localhost:7002
```

## 验证流程

1. 启动 Server: `cd a2a-server-module-rest && mvn spring-boot:run`
2. 启动 Client: `cd a2a-client-module-rest && mvn spring-boot:run`
3. 测试 Agent Card: `curl http://localhost:7001/test/agent-card`
4. 测试发送消息: `curl -X POST "http://localhost:7001/test/send?msg=Hello"`
