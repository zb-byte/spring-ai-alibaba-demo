package com.example.a2aserver.sdk.protocol.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.a2a.spec.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolType;

import java.util.List;

/**
 * JSON-RPC 协议服务器实现
 *
 * 简化实现，直接处理 JSON-RPC 请求
 * 
 * 注意：此类通过工厂创建，但需要手动注册为 Spring Bean 以便 REST 端点生效
 */
@RestController
@RequestMapping
public class JsonRpcProtocolServer extends AbstractProtocolServer {

    /**
     * 构造函数
     * 
     * @param agent Agent 实例
     * @param applicationContext Spring 应用上下文
     * @param properties 服务器配置属性
     */
    public JsonRpcProtocolServer(A2AAgent<?> agent,
                                ApplicationContext applicationContext,
                                A2AServerProperties properties) {
        super(agent, applicationContext, properties);
        this.port = properties.getJsonRpcPort();
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.JSON_RPC;
    }

    @Override
    protected AgentCard buildAgentCard() {
        String serverUrl = getServerUrl() + "/v1/message/send";
        System.out.println("JsonRpcProtocolServer serverUrl: " + serverUrl);
        return new AgentCard.Builder()
                .name(agent.getName())
                .description(agent.getDescription())
                .url(serverUrl)
                .version(agent.getVersion())
                .preferredTransport(TransportProtocol.JSONRPC.name())
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(agent.supportsStreaming())
                        .pushNotifications(false)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("chat")
                                .name("推荐菜品")
                                .description("根据客人口味推荐菜品")
                                .tags(List.of("chat", "json-rpc"))
                                .examples(List.of("推荐一个菜"))
                                .build()
                ))
                .preferredTransport("JSONRPC")
                .protocolVersion("0.3.0")
                .additionalInterfaces(List.of(
                        new AgentInterface("JSONRPC", serverUrl)
                ))
                .build();
    }

    @Override
    protected void doStart(AgentCard agentCard) throws Exception {
        logger.info("JSON-RPC server will be available at: {}/a2a", getServerUrl());
    }

    @Override
    protected void doStop() throws Exception {
        // JSON-RPC 由 Spring 管理
    }
    /**
     * 获取 Agent 卡片
     */
    @GetMapping(value = "/.well-known/agent-card.json", produces = "application/json")
    public Object getAgentCard() {
        try {
            return buildAgentCard();
        } catch (Exception e) {
            logger.error("Error getting agent card", e);
            return Map.of("error", e.getMessage());
        }
    }
    /**
     * 处理 JSON-RPC 请求
     */
    @PostMapping(value = "/v1/message/send", consumes = "application/json", produces = "application/json")
    public Object handleJsonRpcRequest(@RequestBody String request) {
        try {
            // 简单的 JSON-RPC 处理
            String taskId = UUID.randomUUID().toString();
            // 使用 agent 的 createContext 方法来创建正确的上下文类型
            A2AAgent.AgentContext context = (A2AAgent.AgentContext) agent.createContext(Map.of("taskId", taskId));

            // 直接使用请求作为输入 - 显式转换以避免泛型推断问题
            @SuppressWarnings("unchecked")
            A2AAgent<Object> typedAgent = (A2AAgent<Object>) agent;
            A2AAgent.AgentResponse response = typedAgent.execute(request, context);

            // 构建 JSON-RPC 响应
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("jsonrpc", "2.0");
            jsonResponse.put("result", response.getContent());
            jsonResponse.put("id", taskId);

            return jsonResponse;
        } catch (Exception e) {
            logger.error("Error handling JSON-RPC request", e);
            return Map.of(
                "jsonrpc", "2.0",
                "error", Map.of(
                    "code", -32603,
                    "message", "Internal error: " + e.getMessage()
                ),
                "id", null
            );
        }
    }
}
