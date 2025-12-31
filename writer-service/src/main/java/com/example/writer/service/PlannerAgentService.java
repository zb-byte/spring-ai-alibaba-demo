package com.example.writer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;

/**
 * Planner Agent 服务：提供 Agent 调用能力
 * 供 Planner Agent 通过工具函数调用其他 Agent
 */
@Service
public class PlannerAgentService {

    private final AgentRegistry agentRegistry;

    public PlannerAgentService(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    /**
     * 调用本地 Agent（Writer Agent）
     */
    public String invokeLocalAgent(String agentId, String prompt) {
        Optional<AgentRegistry.AgentInfo> agentInfo = agentRegistry.getAgent(agentId);
        if (agentInfo.isEmpty() || !agentInfo.get().isLocal()) {
            return "Agent not found or not a local agent: " + agentId;
        }

        try {
            ReactAgent agent = agentInfo.get().localAgent();
            AssistantMessage message = agent.call(prompt);
            return message.getText();
        } catch (Exception e) {
            return "Error invoking agent: " + e.getMessage();
        }
    }

    /**
     * 通过 A2A 协议调用远程 Agent（Reviewer Agent）
     */
    public String invokeRemoteAgent(String agentId, String prompt) {
        Optional<AgentRegistry.AgentInfo> agentInfo = agentRegistry.getAgent(agentId);
        if (agentInfo.isEmpty() || !agentInfo.get().isRemote()) {
            return "Agent not found or not a remote agent: " + agentId;
        }

        try {
            A2aRemoteAgent agent = agentInfo.get().remoteAgent();
            Optional<OverAllState> state = agent.invoke(prompt);
            return extractResult(state);
        } catch (Exception e) {
            return "Error invoking remote agent: " + e.getMessage();
        }
    }

    /**
     * 获取所有可用 Agent 的能力描述
     */
    public String getAgentCapabilities() {
        return agentRegistry.getAllAgentCapabilities();
    }

    /**
     * 发现所有可用的 Agent
     */
    public String discoverAgents() {
        List<AgentRegistry.AgentInfo> agents = agentRegistry.discoverAgents();
        if (agents.isEmpty()) {
            return "No agents available";
        }

        StringBuilder sb = new StringBuilder("Available agents:\n");
        for (AgentRegistry.AgentInfo agent : agents) {
            sb.append(String.format("- %s (%s): %s [%s]\n",
                    agent.name(), agent.id(), agent.description(),
                    agent.isLocal() ? "Local" : "Remote"));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractResult(Optional<OverAllState> state) {
        if (state.isEmpty()) {
            return "";
        }

        OverAllState s = state.get();

        // 尝试从 outputKey "article" 获取
        if (s.data().containsKey("article")) {
            Object articleObj = s.data().get("article");
            if (articleObj instanceof String) {
                return (String) articleObj;
            }
        }

        // 尝试从 messages 获取
        if (s.data().containsKey("messages")) {
            Object messagesObj = s.data().get("messages");
            if (messagesObj instanceof List) {
                List<Message> messages = (List<Message>) messagesObj;
                return messages.stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(msg -> (AssistantMessage) msg)
                        .reduce((first, second) -> second)
                        .map(AssistantMessage::getText)
                        .orElse("");
            }
        }

        return "";
    }
}

