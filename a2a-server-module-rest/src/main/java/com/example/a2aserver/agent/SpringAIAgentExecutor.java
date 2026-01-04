package com.example.a2aserver.agent;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * Spring AI 驱动的 Agent 执行器
 * 使用 Spring AI ChatClient 调用大模型处理用户消息
 */
@Component
public class SpringAIAgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SpringAIAgentExecutor.class);

    private final ChatClient chatClient;

    public SpringAIAgentExecutor(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个友好的 AI 助手。你会用简洁、有帮助的方式回答用户的问题。")
                .build();
        logger.info("SpringAIAgentExecutor initialized with ChatModel: {}", chatModel.getClass().getSimpleName());
    }

    /**
     * 处理用户消息并返回 AI 响应
     */
    public Message processMessage(MessageSendParams params) {
        String contextId = params.message().getContextId();
        logger.info("Processing message: contextId={}", contextId);

        // 提取用户输入
        String userInput = extractTextFromMessage(params.message());
        logger.debug("User input: {}", userInput);

        if (userInput == null || userInput.trim().isEmpty()) {
            userInput = "Hello";
        }

        // 调用 Spring AI ChatClient 获取响应
        String response;
        try {
            response = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content();
            logger.debug("AI response: {}", response);
        } catch (Exception e) {
            logger.warn("ChatClient call failed, using echo fallback: {}", e.getMessage());
            // 如果 AI 调用失败，使用 Echo 模式
            response = "Echo (AI unavailable): " + userInput;
        }

        // 构建 Agent 响应消息
        return new Message.Builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.AGENT)
                .contextId(contextId)
                .parts(List.of(new TextPart(response)))
                .build();
    }

    /**
     * 流式处理消息 - 返回流式响应
     */
    public void processMessageStreaming(MessageSendParams params, 
                                        java.util.function.Consumer<String> chunkConsumer,
                                        java.util.function.Consumer<Throwable> errorConsumer,
                                        Runnable onComplete) {
        String contextId = params.message().getContextId();
        logger.info("Processing streaming message: contextId={}", contextId);

        String userInput = extractTextFromMessage(params.message());
        if (userInput == null || userInput.trim().isEmpty()) {
            userInput = "Hello";
        }

        try {
            // 使用流式调用
            chatClient.prompt()
                    .user(userInput)
                    .stream()
                    .content()
                    .subscribe(
                            chunk -> {
                                if (chunk != null && !chunk.isEmpty()) {
                                    chunkConsumer.accept(chunk);
                                }
                            },
                            error -> {
                                logger.error("Streaming error", error);
                                errorConsumer.accept(error);
                            },
                            onComplete
                    );
        } catch (Exception e) {
            logger.error("Failed to start streaming", e);
            errorConsumer.accept(e);
        }
    }

    /**
     * 从消息中提取文本内容
     */
    private String extractTextFromMessage(Message message) {
        if (message.getParts() == null || message.getParts().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Part<?> part : message.getParts()) {
            if (part instanceof TextPart textPart) {
                sb.append(textPart.getText());
            }
        }
        return sb.toString();
    }
}
