package com.example.a2aclient.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.spec.AgentCard;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import jakarta.annotation.PostConstruct;

/**
 * A2A REST Client
 * 使用 a2a-java-sdk-client-transport-rest v0.3.3.Final 与 A2A Server 通信
 */
@Component
public class A2ARestClient {

    private static final Logger logger = LoggerFactory.getLogger(A2ARestClient.class);

    @Value("${a2a.server.url:http://localhost:7002}")
    private String serverUrl;

    private AgentCard agentCard;
    private RestTransport transport;

    @PostConstruct
    public void init() {
        logger.info("Initializing A2A REST Client for server: {}", serverUrl);
    }

    /**
     * 获取 Agent Card
     */
    public AgentCard fetchAgentCard() throws Exception {
        logger.info("Fetching agent card from: {}", serverUrl);
        A2ACardResolver resolver = new A2ACardResolver(serverUrl);
        this.agentCard = resolver.getAgentCard();
        this.transport = new RestTransport(agentCard);
        logger.info("Agent card fetched: {}", agentCard.name());
        return agentCard;
    }

    /**
     * 发送消息 (同步)
     */
    public EventKind sendMessage(String text) throws Exception {
        try{
            ensureInitialized();

            logger.info("Sending message: {}", text);

            Message message = new Message.Builder()
                    .messageId(UUID.randomUUID().toString())
                    .role(Message.Role.USER)
                    .contextId(UUID.randomUUID().toString())
                    .parts(List.of(new TextPart(text)))
                    .build();

            MessageSendParams params = new MessageSendParams(message, null, null);

            EventKind result = transport.sendMessage(params, null);
            logger.info("Message sent, result type: {}", result.getClass().getSimpleName());
            return result;
        }
        catch (Exception e) {
            logger.error("Failed to send message", e);
            return null;
        }
    }

    /**
     * 发送消息 (流式)
     */
    public void sendMessageStreaming(String text, Consumer<StreamingEventKind> eventConsumer) throws Exception {
        ensureInitialized();
        
        logger.info("Sending streaming message: {}", text);
        
        Message message = new Message.Builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.USER)
                .contextId(UUID.randomUUID().toString())
                .parts(List.of(new TextPart(text)))
                .build();
        
        MessageSendParams params = new MessageSendParams(message, null, null);
        
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        
        transport.sendMessageStreaming(params, event -> {
            logger.debug("Received streaming event: {}", event.getClass().getSimpleName());
            eventConsumer.accept(event);
            
            if (event instanceof Task task) {
                TaskState state = task.getStatus().state();
                if (state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELED) {
                    completionFuture.complete(null);
                }
            }
        }, error -> {
            logger.error("Streaming error", error);
            completionFuture.completeExceptionally(error);
        }, null);
        
        try {
            completionFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Streaming did not complete within timeout");
        }
    }

    /**
     * 获取 Task
     */
    public Task getTask(String taskId) throws Exception {
        ensureInitialized();
        
        logger.info("Getting task: {}", taskId);
        TaskQueryParams params = new TaskQueryParams(taskId, 0);
        return transport.getTask(params, null);
    }

    /**
     * 取消 Task
     */
    public Task cancelTask(String taskId) throws Exception {
        ensureInitialized();
        
        logger.info("Canceling task: {}", taskId);
        TaskIdParams params = new TaskIdParams(taskId);
        return transport.cancelTask(params, null);
    }

    private void ensureInitialized() throws Exception {
        if (agentCard == null || transport == null) {
            fetchAgentCard();
        }
    }

    public AgentCard getAgentCard() {
        return agentCard;
    }
}
