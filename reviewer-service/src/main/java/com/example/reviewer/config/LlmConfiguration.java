package com.example.reviewer.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
    public ReactAgent reviewerAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder()
                .name("reviewer-agent")
                .description("一个专业的文章评审 Agent，可以对文章进行评审和修改")
                .instruction("""
                        你是一名专业的文章评审员，擅长对文章进行评审和修改。
                        请仔细阅读文章，然后：
                        1. 指出文章的优点
                        2. 指出需要改进的地方
                        3. 提供修改后的完整文章
                        
                        如果文章是关于技术主题的，请确保技术术语使用准确。
                        如果文章是关于文学主题的，请确保文笔流畅、逻辑清晰。
                        
                        最终只返回修改后的完整文章，不要包含评审意见。
                        """)
                .model(chatModel)
                .saver(new MemorySaver())
                .outputKey("article")
                .build();
    }
}

