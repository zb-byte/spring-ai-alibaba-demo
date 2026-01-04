package com.example.a2aclient.demo;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.GetAgentCardRequest;
import io.a2a.spec.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 使用 A2A SDK 封装的客户端调用 gRPC 接口的示例
 * <p>
 * 相比直接使用 gRPC Stub，SDK 封装提供了更简洁的 API 和自动化的功能
 */
public class DirectGrpcStubExample {

    public static void main(String[] args) throws Exception {
        String serverHost = "localhost";
        int grpcPort = 9091;
        String target = serverHost + ":" + grpcPort;

        // 1. 创建 gRPC Channel（SDK 内部会使用）
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        try {
            // 2. 从服务器获取 AgentCard（通过 gRPC 端点，因为服务只有 gRPC）
            System.out.println("通过 gRPC 获取 AgentCard: " + target);
            AgentCard agentCard = getAgentCardViaGrpc(channel);
            System.out.println("成功获取 AgentCard: " + agentCard.name());

            // 3. 创建 A2A 客户端（使用 SDK 封装）
            Client client = Client.builder(agentCard)
                    .clientConfig(new ClientConfig.Builder()
                            .setStreaming(false)  // 使用非流式模式
                            .build())
                    .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                            .channelFactory((String url) -> channel))  // Channel 工厂
                    .build();

            try {
                // 示例 1: 获取 Agent Card
                getAgentCard(client);

                // 示例 2: 发送消息（同步，使用 SDK 封装）
                String taskId = sendMessageSync(client);

                // 示例 3: 发送消息（异步，使用 SDK 封装）
                sendMessageAsync(client);

                Task task = null;
                // 示例 4: 获取任务状态
                if (taskId != null) {
                    task =  getTask(client, taskId);
                }

                // 示例 5: 取消任务
                if (task != null && !TaskState.COMPLETED.equals(task.getStatus().state())) {
                    cancelTask(client, taskId);
                }

            } finally {
                // 关闭客户端
                client.close();
            }

        } finally {
            // 关闭 Channel
            channel.shutdown();
        }
    }

    /**
     * 示例 1: 获取 Agent Card（使用 SDK 封装）
     */
    private static void getAgentCard(Client client) {
        System.out.println("\n=== 示例 1: 获取 Agent Card ===");

        try {
            AgentCard card = client.getAgentCard();
            System.out.println("Agent 名称: " + card.name());
            System.out.println("Agent 描述: " + card.description());
            System.out.println("支持流式: " + card.capabilities().streaming());
        } catch (Exception e) {
            System.err.println("获取 Agent Card 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 示例 2: 同步发送消息（使用 SDK 封装）
     */
    private static String sendMessageSync(Client client) {
        System.out.println("\n=== 示例 2: 同步发送消息 ===");

        try {
            // 使用 A2A 工具类快速创建消息
            Message userMessage = A2A.toUserMessage("你好，请介绍一下你自己");

            // 用于收集响应
            CountDownLatch latch = new CountDownLatch(1);
            StringBuilder response = new StringBuilder();
            String[] taskIdRef = new String[1];

            // 定义事件处理器
            BiConsumer<ClientEvent, AgentCard> eventHandler = (event, card) -> {
                if (event instanceof MessageEvent) {
                    // 收到消息事件
                    MessageEvent msgEvent = (MessageEvent) event;
                    String text = extractText(msgEvent.getMessage());
                    response.append(text);
                    System.out.println("收到消息: " + text);
                } else if (event instanceof TaskEvent) {
                    // 收到任务事件（任务完成）
                    TaskEvent taskEvent = (TaskEvent) event;
                    Task task = taskEvent.getTask();
                    taskIdRef[0] = task.getId();
                    System.out.println("任务完成: " + task.getId() + ", 状态: " + task.getStatus().state());

                    // 从任务的 artifacts 中提取响应
                    if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                        task.getArtifacts().forEach(artifact -> {
                            String text = extractTextFromParts(artifact.parts());
                            if (!text.isEmpty()) {
                                response.append(text);
                                System.out.println("Artifact 内容: " + text);
                            }
                        });
                    }

                    if (task.getStatus().state().isFinal()) {
                        latch.countDown();
                    }
                }
            };

            // 发送消息（SDK 会自动处理流式/非流式）
            client.sendMessage(userMessage, List.of(eventHandler),
                    error -> {
                        System.err.println("错误: " + error.getMessage());
                        latch.countDown();
                    },
                    null);

            // 等待响应（最多 30 秒）
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                System.out.println("等待响应超时");
            }

            System.out.println("最终响应: " + response.toString());
            return taskIdRef[0];

        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 示例 3: 异步发送消息（使用 SDK 封装）
     */
    private static void sendMessageAsync(Client client) {
        System.out.println("\n=== 示例 3: 异步发送消息 ===");

        try {
            // 使用 A2A 工具类快速创建消息
            Message userMessage = A2A.toUserMessage("你好");

            // 用于等待响应
            CountDownLatch latch = new CountDownLatch(1);

            // 定义事件处理器
            BiConsumer<ClientEvent, AgentCard> eventHandler = (event, card) -> {
                if (event instanceof MessageEvent) {
                    MessageEvent msgEvent = (MessageEvent) event;
                    String text = extractText(msgEvent.getMessage());
                    System.out.println("收到消息: " + text);
                } else if (event instanceof TaskEvent) {
                    TaskEvent taskEvent = (TaskEvent) event;
                    Task task = taskEvent.getTask();
                    System.out.println("任务完成: " + task.getId() + ", 状态: " + task.getStatus().state());
                    if (task.getStatus().state().isFinal()) {
                        latch.countDown();
                    }
                }
            };

            // 在后台线程中发送消息
            new Thread(() -> {
                try {
                    client.sendMessage(userMessage, List.of(eventHandler),
                            error -> {
                                System.err.println("错误: " + error.getMessage());
                                latch.countDown();
                            },
                            null);
                } catch (Exception e) {
                    System.err.println("发送消息失败: " + e.getMessage());
                    latch.countDown();
                }
            }).start();

            // 等待响应
            latch.await(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("异步发送消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 示例 4: 获取任务状态（使用 SDK 封装）
     */
    private static Task getTask(Client client, String taskId) {
        System.out.println("\n=== 示例 4: 获取任务状态 ===");

        try {
            TaskQueryParams params = new TaskQueryParams(taskId);
            Task task = client.getTask(params);
            System.out.println("任务 ID: " + task.getId());
            System.out.println("任务状态: " + task.getStatus().state());
            System.out.println("Artifacts 数量: " +
                    (task.getArtifacts() != null ? task.getArtifacts().size() : 0));
            return task;
        } catch (Exception e) {
            System.err.println("获取任务失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 示例 5: 取消任务（使用 SDK 封装）
     */
    private static void cancelTask(Client client, String taskId) {
        System.out.println("\n=== 示例 5: 取消任务 ===");

        try {
            TaskIdParams params = new TaskIdParams(taskId);
            Task cancelledTask = client.cancelTask(params);
            System.out.println("任务已取消: " + cancelledTask.getId());
            System.out.println("最终状态: " + cancelledTask.getStatus().state());
        } catch (Exception e) {
            System.err.println("取消任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通过 gRPC 获取 AgentCard（适用于只有 gRPC 端点的服务）
     */
    private static AgentCard getAgentCardViaGrpc(ManagedChannel channel) throws Exception {
        // 创建阻塞式 Stub
        A2AServiceGrpc.A2AServiceBlockingStub stub = A2AServiceGrpc.newBlockingStub(channel);

        // 调用 gRPC 的 getAgentCard 方法
        GetAgentCardRequest request = GetAgentCardRequest.getDefaultInstance();
        io.a2a.grpc.AgentCard grpcCard = stub.getAgentCard(request);

        // 手动将 gRPC 的 AgentCard 转换为 spec 的 AgentCard（逐字段转换）
        return convertGrpcAgentCardToSpec(grpcCard);
    }

    /**
     * 手动转换 gRPC AgentCard 到 spec AgentCard（v0.3.3.Final 版本）
     */
    private static AgentCard convertGrpcAgentCardToSpec(io.a2a.grpc.AgentCard grpcCard) {
        // 转换 AgentCapabilities
        AgentCapabilities capabilities = null;
        if (grpcCard.hasCapabilities()) {
            io.a2a.grpc.AgentCapabilities grpcCap = grpcCard.getCapabilities();
            capabilities = new AgentCapabilities.Builder()
                    .streaming(grpcCap.getStreaming())
                    .pushNotifications(grpcCap.getPushNotifications())
                    .build();
        }

        // 转换 AgentSkill 列表
        List<AgentSkill> skills = new ArrayList<>();
        if (grpcCard.getSkillsCount() > 0) {
            for (io.a2a.grpc.AgentSkill grpcSkill : grpcCard.getSkillsList()) {
                AgentSkill skill = new AgentSkill.Builder()
                        .id(grpcSkill.getId())
                        .name(grpcSkill.getName())
                        .description(grpcSkill.getDescription())
                        .tags(grpcSkill.getTagsList())
                        .examples(grpcSkill.getExamplesList())
                        .inputModes(grpcSkill.getInputModesList())
                        .outputModes(grpcSkill.getOutputModesList())
                        .build();
                skills.add(skill);
            }
        }

        // 转换 AgentInterface 列表（v0.3.3.Final 可能使用 additionalInterfaces）
        List<AgentInterface> additionalInterfaces = new ArrayList<>();
        if (grpcCard.getAdditionalInterfacesCount() > 0) {
            for (io.a2a.grpc.AgentInterface grpcInterface : grpcCard.getAdditionalInterfacesList()) {
                // v0.3.3.Final 版本：gRPC 使用 getTransport()，spec 使用 (transport, url) 两个参数
                String transport = grpcInterface.getTransport();
                String url = grpcInterface.getUrl();

                AgentInterface agentInterface = new AgentInterface(transport, url);
                additionalInterfaces.add(agentInterface);
            }
        }

        // 转换 AgentProvider
        AgentProvider provider = null;
        if (grpcCard.hasProvider()) {
            io.a2a.grpc.AgentProvider grpcProvider = grpcCard.getProvider();
            provider = new AgentProvider(
                    grpcProvider.getOrganization(),
                    grpcProvider.getUrl()
            );
        }

        // 构建 AgentCard（v0.3.3.Final 使用 url 和 preferredTransport）
        AgentCard.Builder builder = new AgentCard.Builder()
                .name(grpcCard.getName())
                .description(grpcCard.getDescription())
                .version(grpcCard.getVersion())
                .capabilities(capabilities != null ? capabilities :
                        new AgentCapabilities.Builder().build())
                .defaultInputModes(grpcCard.getDefaultInputModesList().isEmpty() ?
                        Collections.singletonList("text") : grpcCard.getDefaultInputModesList())
                .defaultOutputModes(grpcCard.getDefaultOutputModesList().isEmpty() ?
                        Collections.singletonList("text") : grpcCard.getDefaultOutputModesList())
                .skills(skills.isEmpty() ? Collections.emptyList() : skills);

        // v0.3.3.Final 使用 url 和 preferredTransport（已废弃但可能仍在使用）
        builder.url(grpcCard.getUrl());
        builder.preferredTransport(grpcCard.getPreferredTransport());

        // 构建 supportedInterfaces（ClientBuilder 需要这个字段）
        List<AgentInterface> supportedInterfaces = new ArrayList<>();
        if (!additionalInterfaces.isEmpty()) {
            supportedInterfaces.addAll(additionalInterfaces);
            builder.additionalInterfaces(additionalInterfaces);
        } else {
            // 如果没有 additionalInterfaces，根据 preferredTransport 和 url 创建
            String preferredTransport = grpcCard.getPreferredTransport();
            String url = grpcCard.getUrl();
            if (preferredTransport != null && !preferredTransport.isEmpty() && url != null && !url.isEmpty()) {
                AgentInterface interface_ = new AgentInterface(preferredTransport, url);
                supportedInterfaces.add(interface_);
                builder.additionalInterfaces(List.of(interface_));
            }
        }

        // 可选字段
        if (!grpcCard.getDocumentationUrl().isEmpty()) {
            builder.documentationUrl(grpcCard.getDocumentationUrl());
        }
        if (!grpcCard.getProtocolVersion().isEmpty()) {
            builder.protocolVersion(grpcCard.getProtocolVersion());
        }
        if (provider != null) {
            builder.provider(provider);
        }
        return builder.build();
    }

    // 辅助方法：从 Message 中提取文本
    private static String extractText(Message message) {
        if (message.getParts() == null) return "";
        return extractTextFromParts(message.getParts());
    }

    // 辅助方法：从 Parts 列表中提取文本
    private static String extractTextFromParts(List<Part<?>> parts) {
        if (parts == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : parts) {
            if (part instanceof TextPart) {
                sb.append(((TextPart) part).getText());
            }
        }
        return sb.toString();
    }
}
