package com.example.a2aserver.agent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.Artifact;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

/**
 * Spring AI 驱动的 A2A Agent 执行器
 * 
 * 实现 A2A 协议的 AgentExecutor 接口，使用 Spring AI ChatClient 处理消息
 */
@Component
public class SpringAIAgentExecutor implements AgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SpringAIAgentExecutor.class);

    private final ChatClient chatClient;

    public SpringAIAgentExecutor(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个友好的 AI 助手，名叫 Echo Agent。你会用简洁、有帮助的方式回答用户的问题。")
                .build();
        logger.info("SpringAIAgentExecutor initialized with ChatModel: {}", chatModel.getClass().getSimpleName());
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        String taskId = context.getTaskId();
        String contextId = context.getContextId();
        logger.info("Executing agent for task: {}, context: {}", taskId, contextId);

        try {
            // 获取用户输入
            String userInput = context.getUserInput("\n");
            logger.debug("User input: {}", userInput);

            if (userInput == null || userInput.trim().isEmpty()) {
                userInput = "Hello";
            }

            // 发送任务状态更新：WORKING
            TaskStatusUpdateEvent workingStatus = new TaskStatusUpdateEvent.Builder()
                    .taskId(taskId)
                    .contextId(contextId)
                    .status(new TaskStatus(TaskState.WORKING))
                    .isFinal(false)
                    .build();
            eventQueue.enqueueEvent(workingStatus);
            logger.debug("Sent WORKING status for task: {}", taskId);

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
                response = "Echo: " + userInput;
            }

            // 创建 Agent 消息
            Message agentMessage = new Message.Builder()
                    .role(Message.Role.AGENT)
                    .parts(List.of(new TextPart(response)))
                    .messageId(UUID.randomUUID().toString())
                    .taskId(taskId)
                    .contextId(contextId)
                    .build();

            // 发送消息事件
            eventQueue.enqueueEvent(agentMessage);
            logger.debug("Sent agent message for task: {}", taskId);

            // 创建 Artifact
            Artifact artifact = new Artifact.Builder()
                    .artifactId(UUID.randomUUID().toString())
                    .name("response")
                    .parts(List.of(new TextPart(response)))
                    .build();

            // 发送 Artifact 更新事件
            TaskArtifactUpdateEvent artifactEvent = new TaskArtifactUpdateEvent.Builder()
                    .taskId(taskId)
                    .contextId(contextId)
                    .artifact(artifact)
                    .build();
            eventQueue.enqueueEvent(artifactEvent);
            logger.debug("Sent artifact update for task: {}", taskId);

            // 发送任务状态更新：COMPLETED
            Message completedMessage = new Message.Builder()
                    .role(Message.Role.AGENT)
                    .parts(List.of(new TextPart("Task completed successfully")))
                    .messageId(UUID.randomUUID().toString())
                    .build();

            TaskStatusUpdateEvent completedStatus = new TaskStatusUpdateEvent.Builder()
                    .taskId(taskId)
                    .contextId(contextId)
                    .status(new TaskStatus(TaskState.COMPLETED, completedMessage, OffsetDateTime.now(ZoneOffset.UTC)))
                    .isFinal(true)
                    .build();
            eventQueue.enqueueEvent(completedStatus);
            logger.info("Task {} completed successfully", taskId);

        } catch (Exception e) {
            logger.error("Error executing agent for task: {}", taskId, e);

            // 发送失败状态
            Message errorMessage = new Message.Builder()
                    .role(Message.Role.AGENT)
                    .parts(List.of(new TextPart("Error: " + e.getMessage())))
                    .messageId(UUID.randomUUID().toString())
                    .build();

            TaskStatusUpdateEvent failedStatus = new TaskStatusUpdateEvent.Builder()
                    .taskId(taskId)
                    .contextId(contextId)
                    .status(new TaskStatus(TaskState.FAILED, errorMessage, OffsetDateTime.now(ZoneOffset.UTC)))
                    .isFinal(true)
                    .build();
            eventQueue.enqueueEvent(failedStatus);

            throw new io.a2a.spec.InternalError("Agent execution failed: " + e.getMessage());
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        String taskId = context.getTaskId();
        String contextId = context.getContextId();
        logger.info("Cancelling task: {}", taskId);

        // 发送取消状态
        Message cancelMessage = new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(List.of(new TextPart("Task cancelled by user")))
                .messageId(UUID.randomUUID().toString())
                .build();

        TaskStatusUpdateEvent cancelledStatus = new TaskStatusUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.CANCELED, cancelMessage, OffsetDateTime.now(ZoneOffset.UTC)))
                .isFinal(true)
                .build();
        eventQueue.enqueueEvent(cancelledStatus);
        logger.info("Task {} cancelled", taskId);
    }
}
