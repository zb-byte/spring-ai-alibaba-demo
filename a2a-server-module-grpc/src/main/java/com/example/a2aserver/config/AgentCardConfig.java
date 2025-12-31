package com.example.a2aserver.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

/**
 * AgentCard 配置
 * 
 * 定义 A2A Agent 的元数据和能力
 */
@Configuration
public class AgentCardConfig {

    @Value("${server.port:7002}")
    private int httpPort;

    @Value("${agent.name:Spring AI Echo Agent}")
    private String agentName;

    @Value("${agent.description:A Spring AI powered A2A Agent}")
    private String agentDescription;

    @Bean
    public AgentCard agentCard() {
        // 构建 Agent 能力
        AgentCapabilities capabilities = new AgentCapabilities.Builder()
                .streaming(true)
                .pushNotifications(false)
                .stateTransitionHistory(true)
                .build();

        // 构建 Agent 技能列表
        List<AgentSkill> skills = List.of(
                new AgentSkill.Builder()
                        .id("chat")
                        .name("Chat")
                        .description("General conversation and Q&A powered by Spring AI")
                        .tags(List.of("chat", "ai", "conversation"))
                        .examples(List.of("Hello", "What is Spring AI?", "Help me with coding"))
                        .build(),
                new AgentSkill.Builder()
                        .id("echo")
                        .name("Echo")
                        .description("Echoes back the received message (fallback mode)")
                        .tags(List.of("echo", "test"))
                        .examples(List.of("Echo this message"))
                        .build()
        );

        // 使用 Builder 构建 AgentCard
        // v0.3.3.Final 使用 url 字段而不是 supportedInterfaces
        String agentUrl = "http://localhost:" + httpPort;
        
        return new AgentCard.Builder()
                .name(agentName)
                .description(agentDescription)
                .url(agentUrl)
                .version("1.0.0")
                .documentationUrl("https://github.com/example/a2a-spring-ai-agent")
                .capabilities(capabilities)
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(skills)
                .preferredTransport("grpc")
                .build();
    }
}
