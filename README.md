## 项目简介

本示例将原有的 `spring-ai-alibaba-demo` 项目改造成 Maven 结构，并基于 **Spring AI Alibaba** 构建了一个简单的 ReactAgent，展示如何使用通用的 OpenAI 兼容模型（如 `deepseek-v3-250324`）进行对话，并额外演示 **Google Agent-to-Agent (A2A)** 协议的服务端与客户端能力。应用同时暴露：

- `POST /api/chat` 与 `POST /api/chat/a2a`：通过 A2A 协议转发到远端（示例中即自身）ReactAgent。
- `POST /api/chat/local`：本地直连 LLM 的对比接口。

常用配置通过以下环境变量传入：

- `DEFAULT_LLM_API_KEY`
- `DEFAULT_LLM_BASE_URL`
- `DEFAULT_LLM_MODEL_NAME`

```bash
export DEFAULT_LLM_API_KEY="9da16721-41c7-4d52-9876-94bb4171eedf"
export DEFAULT_LLM_BASE_URL="https://ark.cn-beijing.volces.com"
export DEFAULT_LLM_MODEL_NAME="deepseek-v3-250324"
```

示例默认模型名称为 `deepseek-v3-250324`，可根据环境变量覆盖。若后端服务路径不同，可再设置：

```bash
export DEFAULT_LLM_COMPLETIONS_PATH="/api/v3/chat/completions"
export DEFAULT_LLM_EMBEDDINGS_PATH="/api/v3/embeddings"
```

## 运行前置条件

- JDK 17+
- Maven 3.9+
- 可用的 OpenAI 兼容模型服务端
- 正确设置环境变量，例如：

```bash
export DEFAULT_LLM_API_KEY="9da16721-41c7-4d52-9876-94bb4171eedf"
export DEFAULT_LLM_BASE_URL="https://ark.cn-beijing.volces.com"
export DEFAULT_LLM_MODEL_NAME="deepseek-v3-250324"
```

## 启动与体验

```bash
mvn spring-boot:run
```

随后可分别调用本地与 A2A 接口：

```bash
curl -X POST http://localhost:8080/api/chat/local \
  -H "Content-Type: application/json" \
  -d '{"question": "介绍一下Spring AI Alibaba"}'

curl -X POST http://localhost:8080/api/chat/a2a \
  -H "Content-Type: application/json" \
  -d '{"question": "通过A2A转发这条消息"}'
```

## 核心实现

- `pom.xml` 引入了 `spring-ai-alibaba-starter-a2a-nacos`，借助其自动配置提供 JSON-RPC 形式的 A2A 服务端路由（`/.well-known/agent.json` 与 `/a2a`）。
- `LlmConfiguration` 装配 `OpenAiChatModel`、本地 `ReactAgent`（同时作为 A2A 服务端的 Root Agent）以及指向该服务端的 `A2aRemoteAgent` 客户端。
- `AgentService` 同时封装本地直连与 A2A 远程两种调用路径，便于对比。
- `ChatController` 暴露 `/api/chat/local` 与 `/api/chat/a2a` 两个接口，默认 `/api/chat` 也会走 A2A 通道，帮助快速演示协议链路。

## 谷歌 A2A 协议（Google Agent-to-Agent）的使用说明

Spring AI Alibaba 已内置对 **A2A (Agent-to-Agent)** 协议的实现，可实现跨服务的智能体互联。本 Demo 已实现一个最小可运行样例：

1. **服务端自动暴露**  
   引入 `spring-ai-alibaba-starter-a2a-nacos` 后，配置 `spring.ai.alibaba.a2a.server.*` 属性即可让本地 `ReactAgent` 自动挂到 JSON-RPC A2A 服务，生成标准的 `/.well-known/agent.json` 与 `/a2a` 接口。示例默认通过 `A2A_NACOS_DISCOVERY_ENABLED` / `A2A_NACOS_REGISTRY_ENABLED` 环境变量保持关闭，方便单机演示。

2. **客户端远程调用**  
   `LlmConfiguration` 中额外声明了一个 `A2aRemoteAgent`，直接复用服务端暴露的 `AgentCard`。业务层通过它与服务端 HTTP 通信，实现真正的 A2A 请求/响应链路。

3. **接口对比**  
   `POST /api/chat/local` 走本地 `ReactAgent`，`POST /api/chat/a2a` 走远程 `A2aRemoteAgent`。响应体会附加 `mode` 字段，方便演示时区分调用路径。

若要接入真实的多 Agent 环境，可将 `spring.ai.alibaba.a2a.nacos.discovery.enabled` / `registry.enabled` 设为 `true` 并配置好 Nacos，或改用其它 `AgentCardProvider` 实现来发现远端 Agent。

## 公网部署 & 运维建议

1. **配置公网信息**  
   将以下环境变量改成自己的域名 / 路径后再启动应用，A2A `AgentCard` 就会指向公网地址：

   ```bash
   export A2A_SERVER_ADDRESS="agent.example.com"
   export A2A_MESSAGE_PATH="/a2a"
   export A2A_CARD_NAME="public-react-agent"
   export A2A_CARD_DESCRIPTION="A2A Agent running on agent.example.com"
   export A2A_CARD_URL="https://agent.example.com/a2a"
   export A2A_CARD_INTERFACE_URL="https://agent.example.com/a2a"
   ```

   启动后访问 `https://agent.example.com/.well-known/agent.json` 即可看到自动生成的 `AgentCard`。

2. **网络与安全**  
   - 确保防火墙 / 安全组放行对应端口，推荐通过 Nginx/Ingress 等反向代理挂上 HTTPS。  
   - 若需限制调用方，可在反向代理层加鉴权（Basic Auth、JWT、IP 白名单等），或自定义 `A2aRequestHandler` 检查请求头。  
   - 默认示例关闭了 Nacos，如需注册中心，将 `A2A_NACOS_DISCOVERY_ENABLED`、`A2A_NACOS_REGISTRY_ENABLED` 置为 `true`，并在 `spring.ai.alibaba.a2a.nacos.*` 下填写 Nacos 连接信息。

3. **客户端接入**  
   远端服务只需读取你的 `/.well-known/agent.json` 即可获得全部能力描述，示例：

   ```bash
   curl https://agent.example.com/.well-known/agent.json | jq .
   ```

   或在 Spring AI Alibaba 中配置：

   ```yaml
   spring:
     ai:
       alibaba:
         a2a:
           client:
             card:
               well-known-url: https://agent.example.com/.well-known/agent.json
   ```

4. **运维监控**  
   建议结合现有日志/监控体系（如 Prometheus、SLS）追踪 `/a2a` 请求、响应时间、错误率；也可以对流式输出做限流或计费，满足业务 SLA。

