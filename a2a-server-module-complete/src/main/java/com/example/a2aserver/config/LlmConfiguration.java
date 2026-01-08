package com.example.a2aserver.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Bean
    @Primary
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
}
