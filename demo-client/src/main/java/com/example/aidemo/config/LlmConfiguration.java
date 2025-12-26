package com.example.aidemo.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import io.a2a.spec.AgentCard;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Bean
    public OpenAiChatModel openAiChatModel(LlmProperties properties) {
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(properties.apiKey());

        if (StringUtils.hasText(properties.baseUrl())) {
            apiBuilder.baseUrl(properties.baseUrl());
        }

        if (StringUtils.hasText(properties.completionsPath())) {
            apiBuilder.completionsPath(properties.completionsPath());
        }

        if (StringUtils.hasText(properties.embeddingsPath())) {
            apiBuilder.embeddingsPath(properties.embeddingsPath());
        }

        return OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.modelName())
                        .build())
                .openAiApi(apiBuilder.build())
                .build();
    }

    @Bean
    @Primary
    public ReactAgent demoReactAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder()
                .name("demo-react-agent")
                .description("一个使用 Spring AI Alibaba ReactAgent 调用大模型的简单示例。")
                .instruction("""
                        你是一名友好的智能助手，擅长简要解答用户问题。
                        如果用户使用中文提问，请用中文作答；否则保持原语言。
                        输出尽量简洁。
                        """)
                .model(chatModel)
                .saver(new MemorySaver())
                .build();
    }

    @Bean(name = "remoteReactAgent")
    public A2aRemoteAgent remoteReactAgent(AgentCard agentCard) throws GraphStateException {
        return A2aRemoteAgent.builder()
                .name(agentCard.name())
                .description("通过 A2A 远程调用 " + agentCard.name())
                .instruction("请通过远端 ReactAgent 回答用户的问题，保持回答简洁。")
                .agentCard(agentCard)
                .build();
    }
}

