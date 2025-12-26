package com.example.writer.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 注册表：用于发现和管理所有可用的 Agent
 * 提供 Agent 发现、能力查询和选择功能
 */
@Service
public class AgentRegistry {

    private final Map<String, AgentInfo> agents = new HashMap<>();

    public AgentRegistry(ReactAgent writerAgent, A2aRemoteAgent reviewerRemoteAgent) {
        // 注册 Writer Agent
        registerAgent("writer-agent", "writer-agent", 
                "一个专业的文章写作 Agent，可以根据主题生成文章", writerAgent, null);
        
        // 注册 Reviewer Remote Agent
        registerAgent("reviewer-remote-agent", "reviewer-remote-agent",
                "通过 A2A 协议调用 Reviewer Service，可以对文章进行评审和修改", null, reviewerRemoteAgent);
    }

    /**
     * 注册 Agent
     */
    private void registerAgent(String id, String name, String description,
                               ReactAgent localAgent, A2aRemoteAgent remoteAgent) {
        agents.put(id, new AgentInfo(id, name, description, localAgent, remoteAgent));
    }

    /**
     * 发现所有可用的 Agent
     */
    public List<AgentInfo> discoverAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * 根据 ID 获取 Agent 信息
     */
    public Optional<AgentInfo> getAgent(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    /**
     * 根据能力描述查找合适的 Agent
     */
    public List<AgentInfo> findAgentsByCapability(String capability) {
        String lowerCapability = capability.toLowerCase();
        return agents.values().stream()
                .filter(agent -> agent.description().toLowerCase().contains(lowerCapability) ||
                               agent.name().toLowerCase().contains(lowerCapability))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有 Agent 的能力描述（用于 Planner Agent 理解）
     */
    public String getAllAgentCapabilities() {
        return agents.values().stream()
                .map(agent -> String.format("- %s (%s): %s", 
                        agent.name(), agent.id(), agent.description()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Agent 信息
     */
    public record AgentInfo(
            String id,
            String name,
            String description,
            ReactAgent localAgent,
            A2aRemoteAgent remoteAgent
    ) {
        public boolean isLocal() {
            return localAgent != null;
        }

        public boolean isRemote() {
            return remoteAgent != null;
        }
    }
}

