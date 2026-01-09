package com.example.a2aserver.sdk.protocol.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.common.collect.Lists;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.TransportProtocol;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolType;

import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

/**
 * REST 协议服务器实现
 *
 * 直接实现 REST 端点，不依赖 A2A SDK 的 RequestHandler
 */
@RestController
@RequestMapping
public class RestProtocolServer extends AbstractProtocolServer {

    private final Map<String, Object> taskStore = new HashMap<>();

    /**
     * 构造函数
     * 
     * @param agent Agent 实例
     * @param applicationContext Spring 应用上下文
     * @param properties 服务器配置属性
     */
    public RestProtocolServer(A2AAgent<?> agent,
                             ApplicationContext applicationContext,
                             A2AServerProperties properties) {
        super(agent, applicationContext, properties);
        this.port = properties.getRestPort();
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.HTTP_REST;
    }

    @Override
    protected AgentCard buildAgentCard() {
        return new AgentCard.Builder()
                .name(agent.getName() + " (REST)")
                .description(agent.getDescription())
                .url(getServerUrl())
                .version(agent.getVersion())
                .preferredTransport(TransportProtocol.HTTP_JSON.name())
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(agent.supportsStreaming())
                        .build())
                .defaultInputModes(Lists.newArrayList("text"))
                .defaultOutputModes(Lists.newArrayList("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("chat")
                                .name("Chat")
                                .description("Chat with the agent via REST")
                                .tags(List.of("chat", "rest"))
                                .examples(List.of("Hello", "What can you do?", "Help me with a task"))
                                .build()
                ))
                .build();
    }

    @Override
    protected void doStart(AgentCard agentCard) throws Exception {
        logger.info("REST server will be available at: {}", getServerUrl());
    }

    @Override
    protected void doStop() throws Exception {
        // REST 服务器由 Spring Boot 管理
    }

    // ========== REST 端点实现 ==========

    /**
     * 获取 Agent 卡片
     */
    @GetMapping(value = "/.well-known/agent-card.json", produces = "application/json")
    public Object getAgentCard() {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("name", agent.getName() + " (REST)");
            card.put("description", agent.getDescription());
            card.put("version", agent.getVersion());
            card.put("url", getServerUrl());
            card.put("capabilities", Map.of(
                "streaming", agent.supportsStreaming()
            ));
            return card;
        } catch (Exception e) {
            logger.error("Error getting agent card", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 发送消息（同步）
     */
    @PostMapping(value = "/v1/message:send", consumes = "application/json", produces = "application/json")
    public Object sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String taskId = UUID.randomUUID().toString();
            String message = extractMessage(request);

            // 创建上下文
            A2AAgent.AgentContext context = createContext(taskId);

            // 执行 Agent - 显式转换以避免泛型推断问题
            @SuppressWarnings("unchecked")
            A2AAgent<Object> typedAgent = (A2AAgent<Object>) agent;
            A2AAgent.AgentResponse response = typedAgent.execute(message, context);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("content", response.getContent());
            result.put("finished", response.isFinished());
            result.put("metadata", response.getMetadata());

            return result;
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping(value = "/v1/tasks/{taskId}", produces = "application/json")
    public Object getTask(@PathVariable String taskId,
                         @RequestParam(defaultValue = "0") int historyLength) {
        try {
            Object task = taskStore.get(taskId);
            if (task == null) {
                return Map.of("error", "Task not found: " + taskId);
            }
            return task;
        } catch (Exception e) {
            logger.error("Error getting task", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    @PostMapping(value = "/v1/tasks/{taskId}:cancel", produces = "application/json")
    public Object cancelTask(@PathVariable String taskId) {
        try {
            // TODO: 实现任务取消逻辑
            return Map.of(
                "taskId", taskId,
                "status", "canceled"
            );
        } catch (Exception e) {
            logger.error("Error cancelling task", e);
            return Map.of("error", e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    private String extractMessage(Map<String, Object> request) {
        // 尝试从不同的位置提取消息
        if (request.containsKey("message")) {
            return request.get("message").toString();
        }
        if (request.containsKey("content")) {
            return request.get("content").toString();
        }
        if (request.containsKey("params")) {
            Object params = request.get("params");
            if (params instanceof Map) {
                Map<?, ?> paramMap = (Map<?, ?>) params;
                if (paramMap.containsKey("message")) {
                    return paramMap.get("message").toString();
                }
            }
        }
        return request.toString();
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
