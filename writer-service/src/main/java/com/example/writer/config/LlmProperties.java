package com.example.writer.config;

public record LlmProperties(
        String apiKey,
        String baseUrl,
        String modelName,
        String completionsPath,
        String embeddingsPath
) {
}

