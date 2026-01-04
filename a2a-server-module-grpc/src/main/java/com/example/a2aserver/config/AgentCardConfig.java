package com.example.a2aserver.config;

import java.util.Collections;
import java.util.List;

import io.a2a.grpc.AgentCapabilities;
import io.a2a.grpc.AgentCard;
import io.a2a.grpc.AgentSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.a2a.spec.TransportProtocol.GRPC;


/**
 * AgentCard 配置
 * 
 * 定义 A2A Agent 的元数据和能力
 */
@Configuration
public class AgentCardConfig {

    @Value("${agent.name:Spring AI Echo Agent}")
    private String agentName;

    @Value("${agent.description:A Spring AI powered A2A Agent}")
    private String agentDescription;

    @Bean
    public AgentCard agentCard() {
        // 构建 Agent 能力
        AgentCapabilities capabilities =  AgentCapabilities.newBuilder()
                .setStreaming(true)
                .setPushNotifications(false)
                .build();

        // 构建 Agent 技能列表
        List<AgentSkill> skills = List.of(
                  AgentSkill.newBuilder()
                        .setId("chat")
                        .setName("Chat")
                        .setDescription("General conversation and Q&A powered by Spring AI")
                        .addTags("chat")
                        .addAllExamples((List.of("Hello", "What is Spring AI?", "Help me with coding")))
                          .build(),
                  AgentSkill.newBuilder()
                        .setId("echo")
                        .setName("Echo")
                        .setDescription("Echoes back the received message (fallback mode)")
                        .addAllTags(List.of("echo", "test"))
                        .addAllExamples(List.of("Echo this message"))
                        .build()
        );

        // 使用 Builder 构建 AgentCard
        String agentUrl = "http://localhost:9091";
        
        return  AgentCard.newBuilder()
                .setName(agentName)
                .setDescription(agentDescription)
                .setUrl(agentUrl)
                .setVersion("1.0.0")
                .setDocumentationUrl("https://github.com/example/a2a-spring-ai-agent")
                .setCapabilities(capabilities)
                .addDefaultInputModes("text")
                .addDefaultOutputModes("text")
                .addAllSkills(skills)
                .setPreferredTransport(GRPC.name())
                .build();
    }
}
