package com.example.a2aserver.sdk.protocol.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.a2aserver.sdk.agent.A2AAgent;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.AgentCard;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * A2A gRPC 服务委托类
 *
 * 实现 A2A 协议的 gRPC 服务端点
 */
public class A2AGrpcServiceDelegate extends A2AServiceGrpc.A2AServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(A2AGrpcServiceDelegate.class);

    private final A2AAgent<?> agent;
    private final AgentCard agentCard;

    public A2AGrpcServiceDelegate(A2AAgent<?> agent, AgentCard agentCard) {
        this.agent = agent;
        this.agentCard = agentCard;
        logger.info("A2AGrpcServiceDelegate initialized with agent: {}", agent.getName());
    }

    @Override
    public void getAgentCard(io.a2a.grpc.GetAgentCardRequest request,
                            StreamObserver<AgentCard> responseObserver) {
        logger.info("Received getAgentCard request");
        try {
            responseObserver.onNext(agentCard);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing getAgentCard", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sendMessage(SendMessageRequest request,
                           StreamObserver<SendMessageResponse> responseObserver) {
        logger.info("Received sendMessage request");
        try {
            // 提取消息内容
            String message = extractMessage(request);
            
            // 生成任务ID（SendMessageRequest 不包含 taskId，需要自己生成）
            String taskId = UUID.randomUUID().toString();
            
            // 创建上下文
            A2AAgent.AgentContext context = createContext(taskId, request);
            
            // 执行 Agent
            @SuppressWarnings("unchecked")
            A2AAgent<Object> typedAgent = (A2AAgent<Object>) agent;
            A2AAgent.AgentResponse response = typedAgent.execute(message, context);
            
            // 构建响应
            // SendMessageResponse 使用 oneof 模式，可以包含 Task 或 Message
            // 这里使用 Message 来返回响应内容
            io.a2a.grpc.Message responseMessage = io.a2a.grpc.Message.newBuilder()
                    .setTaskId(taskId)
                    .setContextId(context.getContextId())
                    .setRole(io.a2a.grpc.Role.ROLE_AGENT)
                    .addContent(io.a2a.grpc.Part.newBuilder()
                            .setText(response.getContent())
                            .build())
                    .setMetadata(convertMetadataToStruct(response.getMetadata()))
                    .build();
            
            SendMessageResponse grpcResponse = SendMessageResponse.newBuilder()
                    .setMsg(responseMessage)
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("Error processing sendMessage", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error sending message: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
                                     StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        logger.info("Received sendStreamingMessage request");
        // TODO: 实现流式消息逻辑
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void getTask(io.a2a.grpc.GetTaskRequest request,
                       StreamObserver<io.a2a.grpc.Task> responseObserver) {
        logger.info("Received getTask request");
        // TODO: 实现任务获取逻辑
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void cancelTask(io.a2a.grpc.CancelTaskRequest request,
                          StreamObserver<io.a2a.grpc.Task> responseObserver) {
        logger.info("Received cancelTask request");
        // TODO: 实现任务取消逻辑
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request,
                                StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        logger.info("Received taskSubscription request");
        // TODO: 实现任务订阅逻辑
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    // Push notification methods
    @Override
    public void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
                                                  StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
                                               StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
                                                StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    public void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
                                                  StreamObserver<Empty> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    // ========== 辅助方法 ==========

    /**
     * 从 gRPC 请求中提取消息内容
     */
    private String extractMessage(SendMessageRequest request) {
        // SendMessageRequest 包含一个 Message 对象，Message 包含 Part 列表
        if (request.hasRequest()) {
            io.a2a.grpc.Message message = request.getRequest();
            // 从 Message 的 content 列表中提取文本
            StringBuilder textBuilder = new StringBuilder();
            for (io.a2a.grpc.Part part : message.getContentList()) {
                if (!part.getText().isEmpty()) {
                    if (textBuilder.length() > 0) {
                        textBuilder.append(" ");
                    }
                    textBuilder.append(part.getText());
                }
            }
            if (textBuilder.length() > 0) {
                return textBuilder.toString();
            }
        }
        // 如果无法从 Message 中提取，尝试从 metadata 中提取
        if (request.hasMetadata() && request.getMetadata().containsFields("message")) {
            return request.getMetadata().getFieldsOrThrow("message").getStringValue();
        }
        return "Empty message";
    }

    /**
     * 创建 Agent 上下文
     */
    private A2AAgent.AgentContext createContext(String taskId, SendMessageRequest request) {
        return new A2AAgent.AgentContext() {
            private final Map<String, Object> attributes = new HashMap<>();

            @Override
            public String getTaskId() {
                return taskId;
            }

            @Override
            public String getContextId() {
                // SendMessageRequest 不包含 contextId，生成一个新的
                return UUID.randomUUID().toString();
            }

            @Override
            public Map<String, Object> getAttributes() {
                // 将 gRPC 请求的 metadata 转换为 attributes
                if (request.hasMetadata()) {
                    request.getMetadata().getFieldsMap().forEach((key, value) -> {
                        if (value.hasStringValue()) {
                            attributes.put(key, value.getStringValue());
                        } else if (value.hasNumberValue()) {
                            attributes.put(key, value.getNumberValue());
                        } else if (value.hasBoolValue()) {
                            attributes.put(key, value.getBoolValue());
                        }
                    });
                }
                return attributes;
            }
        };
    }

    /**
     * 转换元数据为 gRPC 格式 (Map<String, String>)
     */
    private Map<String, String> convertMetadata(Map<String, Object> metadata) {
        Map<String, String> result = new HashMap<>();
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                if (value != null) {
                    result.put(key, value.toString());
                }
            });
        }
        return result;
    }

    /**
     * 转换元数据为 protobuf Struct 格式
     */
    private Struct convertMetadataToStruct(Map<String, Object> metadata) {
        Struct.Builder structBuilder = Struct.newBuilder();
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                if (value != null) {
                    Value.Builder valueBuilder = Value.newBuilder();
                    if (value instanceof String) {
                        valueBuilder.setStringValue((String) value);
                    } else if (value instanceof Number) {
                        valueBuilder.setNumberValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        valueBuilder.setBoolValue((Boolean) value);
                    } else {
                        valueBuilder.setStringValue(value.toString());
                    }
                    structBuilder.putFields(key, valueBuilder.build());
                }
            });
        }
        return structBuilder.build();
    }
}
