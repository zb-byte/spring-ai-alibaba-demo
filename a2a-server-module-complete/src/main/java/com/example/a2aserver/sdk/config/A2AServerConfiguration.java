package com.example.a2aserver.sdk.config;

import com.example.a2aserver.sdk.model.AgentAuthentication;
import com.example.a2aserver.sdk.server.jsonrpc.JsonRpcProtocolServer;
import com.example.a2aserver.sdk.server.jsonrpc.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.a2a.spec.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

/**
 * A2A server
 */
@Configuration
public class A2AServerConfiguration {

    /**
     * Configure A2AServer bean
     */
    @Bean
    public JsonRpcProtocolServer a2aServer(ObjectMapper objectMapper, ChatModel chatModel) {
        AgentCard agentCard = createTranslationAgentCard();

        TaskHandler taskHandler = createTranslationTaskHandler(chatModel);

        return new JsonRpcProtocolServer(agentCard, taskHandler, objectMapper);
    }

    /**
     * Create translation agent card
     */
    private AgentCard createTranslationAgentCard() {
        AgentProvider provider = new AgentProvider(
            "xiaopeng",
            "https://localhost:7003"
        );

        List<AgentExtension> extensions = Lists.newArrayList();
        AgentCapabilities capabilities = new AgentCapabilities(
            true,  // streaming
            true,  // pushNotifications
            true ,// stateTransitionHistory
                extensions);

        AgentAuthentication authentication = new AgentAuthentication(   List.of("bearer"), null);

        AgentSkill skill = new AgentSkill(
            "ai-translator",
            "AI Translation Service",
            "Professional AI translator supporting multiple languages. Can translate text between various language pairs including English, Chinese, Japanese, French, Spanish, German, and more.",
            List.of("translation", "language", "ai", "multilingual"),
            List.of(
                "Example: Translate 'Hello World' to Chinese",
                "Example: 请把这句话翻译成英文: '你好'",
                "Example: Translate from French to Spanish: 'Bonjour le monde'"
            ),
            List.of("text"),
            List.of("text"),
                null
        );

        return  new AgentCard.Builder()
                .name("AI Translation Bot")
                .description("Professional AI translation service powered by advanced language models. Supports translation between multiple languages with high accuracy and context awareness.")
                .url("http://localhost:7003/a2a")
                .provider(provider)
                .version( "1.0.0")
                .documentationUrl( "http://localhost:7003/docs")
                .capabilities(capabilities)
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(skill))
                .build();

    }

    /**
     * Create translation task handler using ChatClient
     */
    private TaskHandler createTranslationTaskHandler(ChatModel chatModel) {
        ChatClient chatClient = ChatClient.create(chatModel);

        return (task, message) -> {
            try {
                // Extract text content from message parts
                String textToTranslate = extractTextFromMessage(message);

                if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                    return createErrorTask(task, "No text content found in the message");
                }

                // Create translation prompt
                String translationPrompt = createTranslationPrompt(textToTranslate);

                // Call ChatClient for translation
                String translatedText = chatClient
                    .prompt(translationPrompt)
                    .call()
                    .content();

                // Create response message with translation
                TextPart responsePart = new TextPart(translatedText, null);
                Message responseMessage = new Message(Message.Role.AGENT,
                        List.of(responsePart),
                        UUID.randomUUID().toString(),
                        message.getContextId(),
                        task.getId(),
                        null, null,null, Part.Kind.TEXT.name());

                // Create completed status
                TaskStatus completedStatus = new TaskStatus(
                    TaskState.COMPLETED
                );

                // Add response to history
                List<Message> updatedHistory = task.getHistory() != null ?
                    List.of(task.getHistory().toArray(new Message[0])) :
                    List.of();
                updatedHistory = List.of(
                    java.util.stream.Stream.concat(
                        updatedHistory.stream(),
                        java.util.stream.Stream.of(message, responseMessage)
                    ).toArray(Message[]::new)
                );

                return new Task(
                    task.getId(),
                    task.getContextId(),
                    completedStatus,
                    task.getArtifacts(),
                    updatedHistory,
                    task.getMetadata(),
                    task.getKind()
                );

            } catch (Exception e) {
                return createErrorTask(task, "Translation failed: " + e.getMessage());
            }
        };
    }

    /**
     * Extract text content from message parts
     */
    private String extractTextFromMessage(Message message) {
        if (message.getParts() == null || message.getParts().isEmpty()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Part part : message.getParts()) {
            if (part instanceof TextPart textPart) {
                if (!textBuilder.isEmpty()) {
                    textBuilder.append("\n");
                }
                textBuilder.append(textPart.getText());
            }
        }

        return textBuilder.toString();
    }

    /**
     * Create translation prompt for ChatClient
     */
    private String createTranslationPrompt(String text) {
        return String.format("""
            You are a professional translator. Please translate the following text to the most appropriate target language.
            
            Instructions:
            1. If the text is in Chinese, translate to English
            2. If the text is in English, translate to Chinese
            3. If the text is in other languages, translate to English
            4. Maintain the original meaning and context
            5. Provide natural, fluent translations
            6. Only return the translated text, no explanations
            
            Text to translate: %s
            """, text);
    }

    /**
     * Create error task for translation failures
     */
    private Task createErrorTask(Task originalTask, String errorMessage) {
        TaskStatus errorStatus = new TaskStatus(
            TaskState.FAILED
        );

        return new Task(
            originalTask.getId(),
            originalTask.getContextId(),
            errorStatus,
            originalTask.getArtifacts(),
            originalTask.getHistory(),
            originalTask.getMetadata(),
            originalTask.getKind()
        );
    }
}
