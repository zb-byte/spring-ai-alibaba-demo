# A2A gRPC Client Demo


## 快速开始

### 1. 启动 A2A Server

```bash
cd ../a2a-server-module-grpc
启动 A2aServerModuleApplication.java
```

Server 将在以下端口启动：
- gRPC: 9091

### 2. 执行客户端测试

```bash
cd ../a2a-client-module-grpc
执行 DirectGrpcStubExample.java 
```

## 核心组件

## 通过 gRPC 协议完成 A2A 通信

本模块提供了完整的示例代码 `DirectGrpcStubExample.java`，演示了如何使用 A2A SDK 通过 gRPC 协议与 A2A 服务器进行通信。

### 核心步骤

#### 1. 创建 gRPC Channel

首先需要创建一个 gRPC Channel 连接到服务器：

```java
String serverHost = "localhost";
int grpcPort = 9091;
String target = serverHost + ":" + grpcPort;

ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        .usePlaintext()  // 开发环境使用，生产环境应使用 TLS
        .build();
```

#### 2. 通过 gRPC 获取 AgentCard

当服务提供方只有 gRPC 端点时，需要通过 gRPC 直接获取 AgentCard：

```java
// 创建阻塞式 Stub
A2AServiceGrpc.A2AServiceBlockingStub stub = A2AServiceGrpc.newBlockingStub(channel);

// 调用 gRPC 的 getAgentCard 方法
GetAgentCardRequest request = GetAgentCardRequest.getDefaultInstance();
io.a2a.grpc.AgentCard grpcCard = stub.getAgentCard(request);

// 将 gRPC 的 AgentCard 转换为 spec 的 AgentCard
AgentCard agentCard = convertGrpcAgentCardToSpec(grpcCard);
```

**注意**：v0.3.3.Final 版本需要手动转换 gRPC AgentCard 到 spec AgentCard，因为：
- gRPC 版本使用 `getTransport()` 方法
- spec 版本使用 `protocolBinding` 字段
- 需要将 `additionalInterfaces` 转换为 `supportedInterfaces`（ClientBuilder 需要）

#### 3. 创建 A2A 客户端

使用获取到的 AgentCard 创建 A2A 客户端：

```java
Client client = Client.builder(agentCard)
        .clientConfig(new ClientConfig.Builder()
                .setStreaming(false)  // 使用非流式模式
                .build())
        .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                .channelFactory((String url) -> channel))  // 提供 Channel 工厂
        .build();
```

**关键点**：
- 使用 `GrpcTransport.class` 指定 gRPC 传输协议
- 通过 `channelFactory` 提供已创建的 Channel，避免重复创建

#### 4. 发送消息（同步）

同步发送消息并等待响应：

```java
// 创建用户消息
Message userMessage = A2A.toUserMessage("你好，请介绍一下你自己");

// 用于收集响应
CountDownLatch latch = new CountDownLatch(1);
StringBuilder response = new StringBuilder();
String[] taskIdRef = new String[1];

// 定义事件处理器
BiConsumer<ClientEvent, AgentCard> eventHandler = (event, card) -> {
    if (event instanceof MessageEvent) {
        // 收到消息事件
        MessageEvent msgEvent = (MessageEvent) event;
        String text = extractText(msgEvent.getMessage());
        response.append(text);
        System.out.println("收到消息: " + text);
    } else if (event instanceof TaskEvent) {
        // 收到任务事件（任务完成）
        TaskEvent taskEvent = (TaskEvent) event;
        Task task = taskEvent.getTask();
        taskIdRef[0] = task.getId();
        
        // 从任务的 artifacts 中提取响应
        if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
            task.getArtifacts().forEach(artifact -> {
                String text = extractTextFromParts(artifact.parts());
                if (!text.isEmpty()) {
                    response.append(text);
                }
            });
        }
        
        if (task.getStatus().state().isFinal()) {
            latch.countDown();
        }
    }
};

// 发送消息
client.sendMessage(userMessage, List.of(eventHandler),
        error -> {
            System.err.println("错误: " + error.getMessage());
            latch.countDown();
        },
        null);

// 等待响应（最多 30 秒）
boolean completed = latch.await(30, TimeUnit.SECONDS);
String taskId = taskIdRef[0];
```

#### 5. 发送消息（异步）

在后台线程中异步发送消息：

```java
Message userMessage = A2A.toUserMessage("你好");
CountDownLatch latch = new CountDownLatch(1);

BiConsumer<ClientEvent, AgentCard> eventHandler = (event, card) -> {
    if (event instanceof MessageEvent) {
        MessageEvent msgEvent = (MessageEvent) event;
        String text = extractText(msgEvent.getMessage());
        System.out.println("收到消息: " + text);
    } else if (event instanceof TaskEvent) {
        TaskEvent taskEvent = (TaskEvent) event;
        Task task = taskEvent.getTask();
        if (task.getStatus().state().isFinal()) {
            latch.countDown();
        }
    }
};

// 在后台线程中发送消息
new Thread(() -> {
    try {
        client.sendMessage(userMessage, List.of(eventHandler),
                error -> {
                    System.err.println("错误: " + error.getMessage());
                    latch.countDown();
                },
                null);
    } catch (Exception e) {
        System.err.println("发送消息失败: " + e.getMessage());
        latch.countDown();
    }
}).start();

// 等待响应
latch.await(30, TimeUnit.SECONDS);
```

#### 6. 获取任务状态

根据任务 ID 查询任务状态：

```java
TaskQueryParams params = new TaskQueryParams(taskId);
Task task = client.getTask(params);
System.out.println("任务 ID: " + task.getId());
System.out.println("任务状态: " + task.getStatus().state());
System.out.println("Artifacts 数量: " + 
        (task.getArtifacts() != null ? task.getArtifacts().size() : 0));
```

#### 7. 取消任务

取消正在执行的任务：

```java
TaskIdParams params = new TaskIdParams(taskId);
Task cancelledTask = client.cancelTask(params);
System.out.println("任务已取消: " + cancelledTask.getId());
System.out.println("最终状态: " + cancelledTask.getStatus().state());
```


