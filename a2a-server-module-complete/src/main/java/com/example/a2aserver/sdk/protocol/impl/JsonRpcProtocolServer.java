package com.example.a2aserver.sdk.protocol.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolType;

import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.AgentInterface;

import java.util.List;

/**
 * JSON-RPC 协议服务器实现
 *
 * 简化实现，直接处理 JSON-RPC 请求
 */
@Component
@RestController
@RequestMapping("/a2a")
public class JsonRpcProtocolServer extends AbstractProtocolServer {

    @Autowired(required = false)
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
        String serverUrl = getServerUrl() + "/a2a";
        
        return new AgentCard.Builder()
                .name(agent.getName() + " (JSON-RPC)")
                .description(agent.getDescription())
                .url(serverUrl)
                .version(agent.getVersion())
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
                                .name("Chat")
                                .description("Chat with the agent via JSON-RPC")
                                .tags(List.of("chat", "json-rpc"))
                                .examples(List.of("Hello", "What can you do?", "Help me with a task"))
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
     * 处理 JSON-RPC 请求
     */
    @PostMapping
    public Object handleJsonRpcRequest(@RequestBody String request) {
        try {
            // 简单的 JSON-RPC 处理
            String taskId = UUID.randomUUID().toString();
            A2AAgent.AgentContext context = createContext(taskId);

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

    private A2AAgent.AgentContext createContext(String taskId) {
        return new A2AAgent.AgentContext() {
            private final Map<String, Object> attributes = new HashMap<>();

            @Override
            public String getTaskId() {
                return taskId;
            }

            @Override
            public String getContextId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public Map<String, Object> getAttributes() {
                return attributes;
            }
        };
    }
}
