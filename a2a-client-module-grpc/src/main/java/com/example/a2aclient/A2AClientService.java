package com.example.a2aclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * A2A gRPC 客户端服务
 * 
 * 基于 A2A Java SDK v0.3.3.Final 实现的真实 gRPC 客户端服务
 * 提供与 A2A gRPC 服务器的通信功能
 */
@Service
public class A2AClientService {

    private static final Logger logger = LoggerFactory.getLogger(A2AClientService.class);

    @Value("${a2a.server.host:localhost}")
    private String serverHost;

    @Value("${a2a.server.grpc-port:9090}")
    private int grpcPort;

    private ManagedChannel channel;
    private Client a2aClient;
    private AgentCard agentCard;

    @PostConstruct
    public void init() {
        logger.info("A2AClientService initializing...");
        // 延迟初始化，等待服务器启动
    }

    /**
     * 初始化 A2A 客户端连接
     */
    public synchronized void initializeClient() {
        logger.info("初始化 A2A gRPC 客户端连接...");
        logger.info("目标服务器: {}:{}", serverHost, grpcPort);

        try {
            // 关闭旧连接
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown();
            }

            // 创建 gRPC Channel
            String target = serverHost + ":" + grpcPort;
            channel = ManagedChannelBuilder.forTarget(target)
                    .usePlaintext()
                    .build();

            // 创建 AgentCard（用于客户端配置）
            // v0.3.3.Final 使用 new Builder() 构造器模式
            AgentCapabilities capabilities = new AgentCapabilities.Builder()
                    .streaming(true)
                    .pushNotifications(false)
                    .build();

            agentCard = new AgentCard.Builder()
                    .name("Echo Agent")
                    .description("A2A Echo Agent Demo")
                    .version("1.0.0")
                    .url("grpc://" + target)
                    .capabilities(capabilities)
                    .defaultInputModes(Collections.singletonList("text"))
                    .defaultOutputModes(Collections.singletonList("text"))
                    .build();

            // 创建 A2A Client
            a2aClient = Client.builder(agentCard)
                    .clientConfig(new ClientConfig.Builder()
                            .setStreaming(false)  // 使用非流式模式
                            .build())
                    .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                            .channelFactory(t -> channel))
                    .build();

            logger.info("✅ A2A gRPC 客户端初始化完成");

        } catch (Exception e) {
            logger.error("❌ A2A gRPC 客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize A2A client", e);
        }
    }

    /**
     * 确保客户端已初始化
     */
    private void ensureInitialized() {
        if (a2aClient == null) {
            initializeClient();
        }
    }

    /**
     * 发送消息到 A2A 服务器
     * 
     * @param messageText 要发送的消息文本
     * @return 异步响应
     */
    public CompletableFuture<String> sendMessage(String messageText) {
        logger.info("发送消息到 A2A 服务器: {}", messageText);

        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureInitialized();

                // 创建用户消息 - v0.3.3.Final 使用 new Builder() 模式
                Message userMessage = new Message.Builder()
                        .role(Message.Role.USER)
                        .parts(List.of(new TextPart(messageText)))
                        .build();

                // 用于收集响应
                CountDownLatch latch = new CountDownLatch(1);
                List<String> responses = new ArrayList<>();

                // 创建事件消费者
                BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
                    logger.debug("Received event: {}", event.getClass().getSimpleName());
                    
                    if (event instanceof MessageEvent) {
                        MessageEvent msgEvent = (MessageEvent) event;
                        Message msg = msgEvent.getMessage();
                        // v0.3.3.Final Message 是普通类，使用 getParts() getter 方法
                        String text = extractTextFromParts(msg.getParts());
                        responses.add(text);
                        logger.debug("Message event: {}", text);
                    } else if (event instanceof TaskEvent) {
                        TaskEvent taskEvent = (TaskEvent) event;
                        Task task = taskEvent.getTask();
                        // v0.3.3.Final Task 是普通类，使用 getter 方法
                        logger.debug("Task event: taskId={}, status={}", 
                                task.getId(), task.getStatus().state());
                        
                        // 从 Task 的 artifacts 中提取响应
                        // v0.3.3.Final Task.getArtifacts() 返回 List<Artifact>
                        // Artifact 是 record 类型，使用 parts() 方法
                        if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                            task.getArtifacts().forEach(artifact -> {
                                String text = extractTextFromParts(artifact.parts());
                                if (!text.isEmpty()) {
                                    responses.add(text);
                                }
                            });
                        }
                        
                        // 检查任务是否完成
                        if (task.getStatus().state().isFinal()) {
                            latch.countDown();
                        }
                    } else if (event instanceof TaskUpdateEvent) {
                        TaskUpdateEvent updateEvent = (TaskUpdateEvent) event;
                        logger.debug("Task update event: {}", updateEvent);
                        
                        Task task = updateEvent.getTask();
                        if (task != null && task.getStatus().state().isFinal()) {
                            latch.countDown();
                        }
                    }
                };

                // 发送消息
                a2aClient.sendMessage(userMessage, List.of(consumer), 
                        error -> logger.error("Streaming error: {}", error.getMessage()), 
                        null);

                // 等待响应（最多 30 秒）
                boolean completed = latch.await(30, TimeUnit.SECONDS);
                
                if (!completed) {
                    logger.warn("等待响应超时");
                }

                // 组合所有响应
                String finalResponse = responses.isEmpty() 
                        ? "No response received" 
                        : String.join("\n", responses);
                
                logger.info("收到 A2A 服务器响应: {}", finalResponse);
                return finalResponse;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("消息发送被中断: {}", e.getMessage());
                return "Error: 消息发送被中断";
            } catch (Exception e) {
                logger.error("消息发送失败: {}", e.getMessage(), e);
                return "Error: " + e.getMessage();
            }
        });
    }

    /**
     * 从 Parts 中提取文本内容
     * v0.3.3.Final TextPart 使用 getText() getter 方法
     */
    private String extractTextFromParts(List<Part<?>> parts) {
        if (parts == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : parts) {
            if (part instanceof TextPart) {
                TextPart textPart = (TextPart) part;
                sb.append(textPart.getText());
            }
        }
        return sb.toString();
    }

    /**
     * 获取 Agent Card 信息
     * 
     * @return Agent Card JSON 字符串
     */
    public CompletableFuture<String> getAgentCard() {
        logger.info("获取 Agent Card 信息...");

        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureInitialized();

                // 通过 gRPC 获取 Agent Card
                AgentCard card = a2aClient.getAgentCard(null);
                
                // 转换为 JSON 格式 - v0.3.3.Final AgentCard 使用 record 风格方法
                String agentCardJson = String.format("""
                        {
                          "name": "%s",
                          "description": "%s",
                          "version": "%s",
                          "url": "%s",
                          "capabilities": {
                            "streaming": %s,
                            "pushNotifications": %s
                          },
                          "defaultInputModes": %s,
                          "defaultOutputModes": %s
                        }
                        """,
                        card.name(),
                        card.description() != null ? card.description() : "",
                        card.version(),
                        card.url(),
                        card.capabilities().streaming(),
                        card.capabilities().pushNotifications(),
                        formatList(card.defaultInputModes()),
                        formatList(card.defaultOutputModes())
                );

                logger.info("✅ Agent Card 获取成功");
                return agentCardJson;

            } catch (Exception e) {
                logger.error("Agent Card 获取失败: {}", e.getMessage(), e);
                return "Error: " + e.getMessage();
            }
        });
    }

    private String formatList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\", \"", list) + "\"]";
    }

    /**
     * 测试与服务器的连接
     * 
     * @return 连接状态
     */
    public boolean testConnection() {
        logger.info("测试 A2A gRPC 服务器连接...");

        try {
            // 简单的端口连接测试
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(serverHost, grpcPort), 3000);
                logger.info("✅ A2A gRPC 服务器连接成功");
                return true;
            }
        } catch (Exception e) {
            logger.warn("⚠️ A2A gRPC 服务器连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 关闭客户端连接
     */
    @PreDestroy
    public void shutdown() {
        logger.info("关闭 A2A gRPC 客户端连接...");

        try {
            if (a2aClient != null) {
                a2aClient.close();
                a2aClient = null;
            }

            if (channel != null && !channel.isShutdown()) {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                channel = null;
            }

            logger.info("✅ A2A gRPC 客户端已关闭");

        } catch (Exception e) {
            logger.error("❌ A2A gRPC 客户端关闭失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取连接状态信息
     */
    public String getConnectionInfo() {
        return String.format("Server: %s:%d, Connected: %s", 
                serverHost, grpcPort, 
                (channel != null && !channel.isShutdown()));
    }
}
