package com.example.a2aserver.sdk.protocol.impl;

import com.example.a2aserver.sdk.agent.A2AAgent;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.AgentCard;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Empty;

/**
 * A2A gRPC 服务委托类
 *
 * 简化的 gRPC 服务实现
 */
public class A2AGrpcServiceDelegate extends A2AServiceGrpc.A2AServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(A2AGrpcServiceDelegate.class);

    private final A2AAgent<?> agent;
    private final AgentCard agentCard;

    public A2AGrpcServiceDelegate(A2AAgent<?> agent) {
        this.agent = agent;
        this.agentCard = buildAgentCard();
    }

    private AgentCard buildAgentCard() {
        return AgentCard.newBuilder()
                .setName(agent.getName())
                .setDescription(agent.getDescription())
                .setVersion(agent.getVersion())
                .build();
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
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        logger.info("Received sendMessage request");
        // TODO: 实现消息发送逻辑
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
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
}
