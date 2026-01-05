package com.example.a2aserver.grpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.protobuf.Empty;

import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.GetAgentCardRequest;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.TaskSubscriptionRequest;
import io.a2a.grpc.utils.ProtoUtils.FromProto;
import io.a2a.grpc.utils.ProtoUtils.ToProto;
import io.a2a.spec.Task;
import io.a2a.spec.TaskQueryParams;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * A2A gRPC 服务实现
 * 
 * 实现 A2A 协议的 gRPC 服务端点
 */
@Service
public class A2AGrpcService extends A2AServiceGrpc.A2AServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(A2AGrpcService.class);

    private final io.a2a.grpc.AgentCard agentCard;
    // 任务存储
    private final ConcurrentMap<String, Task> taskStore = new ConcurrentHashMap<>();

    public A2AGrpcService(io.a2a.grpc.AgentCard agentCard) {
        this.agentCard = agentCard;
        logger.info("A2AGrpcService initialized");
    }

    @Override
    public void sendMessage(io.a2a.grpc.SendMessageRequest request, 
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        logger.info("Received sendMessage request");
    }

    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request, 
                                     StreamObserver<StreamResponse> responseObserver) {
        logger.info("Received sendStreamingMessage request");
    }

    @Override
    public void getTask(io.a2a.grpc.GetTaskRequest request, 
                       StreamObserver<io.a2a.grpc.Task> responseObserver) {
        logger.info("Received getTask request for name: {}", request.getName());

        try {
            TaskQueryParams params = FromProto.taskQueryParams(request);
            Task task = taskStore.get(params.id());

            if (task == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Task not found: " + params.id())
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(ToProto.task(task));
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error processing getTask", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting task: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // listTasks 方法在 v0.3.3.Final 的 gRPC 服务中不存在
    // 如果需要列出任务，可以通过 HTTP 端点实现

    @Override
    public void cancelTask(io.a2a.grpc.CancelTaskRequest request, 
                          StreamObserver<io.a2a.grpc.Task> responseObserver) {
        logger.info("Received cancelTask request for name: {}", request.getName());
    }

    @Override
    public void taskSubscription(TaskSubscriptionRequest request, 
                                StreamObserver<StreamResponse> responseObserver) {
        logger.info("Received taskSubscription request for name: {}", request.getName());
    }

    @Override
    public void getAgentCard(GetAgentCardRequest request, 
                            StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
        logger.info("Received getAgentCard request");

        try {
            responseObserver.onNext(agentCard);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing getAgentCard", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting agent card: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Push notification methods - 简化实现
    @Override
    public void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
                                                  StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Push notifications not implemented")
                .asRuntimeException());
    }

    @Override
    public void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
                                               StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Push notifications not implemented")
                .asRuntimeException());
    }

    @Override
    public void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
                                                StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Push notifications not implemented")
                .asRuntimeException());
    }

    @Override
    public void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
                                                  StreamObserver<Empty> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Push notifications not implemented")
                .asRuntimeException());
    }
}
