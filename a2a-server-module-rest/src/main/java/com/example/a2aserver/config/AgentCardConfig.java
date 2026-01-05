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
 * 基于 A2A Java SDK v0.3.3.Final
 */
@Configuration
public class AgentCardConfig {

    @Value("${server.port:7002}")
    private int serverPort;

    @Value("${agent.name:Spring AI Chat Agent}")
    private String agentName;

    @Value("${agent.description:A Spring AI powered A2A Agent}")
    private String agentDescription;

    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name(agentName)
                .description(agentDescription)
                .url("http://localhost:" + serverPort)
                .version("1.0.0")
                .documentationUrl("https://github.com/a2aproject/a2a-java")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("chat")
                                .name("Chat with AI")
                                .description("Chat with a large language model powered by Spring AI")
                                .tags(List.of("chat", "ai", "llm"))
                                .examples(List.of("Hello", "What is the weather today?", "Tell me a joke"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("translate")
                                .name("Translation")
                                .description("Translate text between languages")
                                .tags(List.of("translate", "language"))
                                .examples(List.of("Translate 'Hello' to Chinese", "把这句话翻译成英文"))
                                .build()
                ))
                .preferredTransport("REST")
                .additionalInterfaces(List.of(
                        new io.a2a.spec.AgentInterface("REST", "http://localhost:" + serverPort)
                ))
                .build();
    }
}
