package com.example.reviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        String apiKey,
        String baseUrl,
        String modelName,
        String completionsPath,
        String embeddingsPath
) {
}

