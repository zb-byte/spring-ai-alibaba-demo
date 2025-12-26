package com.example.writer.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Value("${reviewer.agent.url:http://127.0.0.1:8081}")
    private String reviewerAgentUrl;

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
    public ReactAgent writerAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder()
                .name("writer-agent")
                .description("一个专业的文章写作 Agent，可以根据主题生成文章")
                .instruction("""
                        你是一名专业的文章写手，擅长根据主题创作高质量的文章。
                        请根据用户提供的主题，创作一篇结构清晰、内容丰富的文章。
                        文章应该：
                        1. 有明确的主题和观点
                        2. 逻辑清晰，层次分明
                        3. 语言流畅，表达准确
                        4. 字数控制在 200-300 字左右
                        
                        直接返回文章内容，不要包含其他说明。
                        """)
                .model(chatModel)
                .saver(new MemorySaver())
                .outputKey("article")
                .build();
    }

    @Bean
    public AgentCardProvider reviewerAgentCardProvider() {
        // 从 Reviewer Service 的 well-known URL 获取 Agent Card
        String wellKnownUrl = reviewerAgentUrl + "/.well-known/agent.json";
        return RemoteAgentCardProvider.newProvider(wellKnownUrl);
    }

    @Bean
    public A2aRemoteAgent reviewerRemoteAgent(AgentCardProvider reviewerAgentCardProvider) throws GraphStateException {
        return A2aRemoteAgent.builder()
                .name("reviewer-remote-agent")
                .description("通过 A2A 协议调用 Reviewer Service")
                .instruction("请对文章进行评审和修改，确保文章质量。最终只返回修改后的完整文章，不要包含评审意见。")
                .agentCardProvider(reviewerAgentCardProvider)
                .outputKey("article")
                .build();
    }
}

