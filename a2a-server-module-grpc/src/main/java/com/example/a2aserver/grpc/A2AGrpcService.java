package com.example.a2aserver.grpc;

import com.google.protobuf.Empty;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.GetAgentCardRequest;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.TaskSubscriptionRequest;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueClosedException;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.QueueManager;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;

/**
 * A2A gRPC 服务实现
 * 
 * 实现 A2A 协议的 gRPC 服务端点
 */
@Service
public class A2AGrpcService extends A2AServiceGrpc.A2AServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(A2AGrpcService.class);

    private final AgentExecutor agentExecutor;
    private final io.a2a.spec.AgentCard agentCard;
    private final QueueManager queueManager;
    private final ExecutorService executorService;

    // 任务存储
    private final ConcurrentMap<String, Task> taskStore = new ConcurrentHashMap<>();

    public A2AGrpcService(AgentExecutor agentExecutor, 
                          io.a2a.spec.AgentCard agentCard,
                          QueueManager queueManager) {
        this.agentExecutor = agentExecutor;
        this.agentCard = agentCard;
        this.queueManager = queueManager;
        this.executorService = Executors.newCachedThreadPool();
        logger.info("A2AGrpcService initialized");
    }

    @Override
    public void sendMessage(io.a2a.grpc.SendMessageRequest request, 
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        logger.info("Received sendMessage request");

        try {
            MessageSendParams params = FromProto.messageSendParams(request);
            Message message = params.message();

            // 生成或获取 taskId 和 contextId
            String taskId = message.getTaskId() != null ? message.getTaskId() : UUID.randomUUID().toString();
            String contextId = message.getContextId() != null ? message.getContextId() : UUID.randomUUID().toString();

            logger.debug("Processing message for task: {}, context: {}", taskId, contextId);

            // 创建事件队列
            EventQueue eventQueue = queueManager.createOrTap(taskId);

            // 异步执行 Agent
            final String finalTaskId = taskId;
            final String finalContextId = contextId;
            CompletableFuture.runAsync(() -> {
                try {
                    // 创建请求上下文
                    RequestContext requestContext = new RequestContext.Builder()
                            .setParams(params)
                            .setTaskId(finalTaskId)
                            .setContextId(finalContextId)
                            .build();
                    agentExecutor.execute(requestContext, eventQueue);
                } catch (JSONRPCError e) {
                    logger.error("Agent execution error", e);
                    // 发送错误状态
                    TaskStatusUpdateEvent errorStatus = new TaskStatusUpdateEvent.Builder()
                            .taskId(finalTaskId)
                            .contextId(finalContextId)
                            .status(new TaskStatus(TaskState.FAILED))
                            .isFinal(true)
                            .build();
                    eventQueue.enqueueEvent(errorStatus);
                } finally {
                    eventQueue.close();
                }
            }, executorService);

            // 等待队列开始轮询
            try {
                queueManager.awaitQueuePollerStart(eventQueue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 收集所有事件并构建最终 Task
            Task finalTask = collectEventsAndBuildTask(taskId, contextId, message, eventQueue);

            // 存储任务
            taskStore.put(taskId, finalTask);

            // 构建响应
            io.a2a.grpc.SendMessageResponse response = ToProto.taskOrMessage(finalTask);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("sendMessage completed for task: {}", taskId);

        } catch (Exception e) {
            logger.error("Error processing sendMessage", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error processing message: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request, 
                                     StreamObserver<StreamResponse> responseObserver) {
        logger.info("Received sendStreamingMessage request");

        if (!agentCard.capabilities().streaming()) {
            responseObserver.onError(Status.UNIMPLEMENTED
                    .withDescription("Streaming not supported")
                    .asRuntimeException());
            return;
        }

        try {
            MessageSendParams params = FromProto.messageSendParams(request);
            Message message = params.message();

            String taskId = message.getTaskId() != null ? message.getTaskId() : UUID.randomUUID().toString();
            String contextId = message.getContextId() != null ? message.getContextId() : UUID.randomUUID().toString();

            logger.debug("Processing streaming message for task: {}, context: {}", taskId, contextId);

            // 创建事件队列
            EventQueue eventQueue = queueManager.createOrTap(taskId);

            // 异步执行 Agent
            final String finalTaskId = taskId;
            final String finalContextId = contextId;
            CompletableFuture.runAsync(() -> {
                try {
                    // 创建请求上下文
                    RequestContext requestContext = new RequestContext.Builder()
                            .setParams(params)
                            .setTaskId(finalTaskId)
                            .setContextId(finalContextId)
                            .build();
                    agentExecutor.execute(requestContext, eventQueue);
                } catch (JSONRPCError e) {
                    logger.error("Agent execution error", e);
                    TaskStatusUpdateEvent errorStatus = new TaskStatusUpdateEvent.Builder()
                            .taskId(finalTaskId)
                            .contextId(finalContextId)
                            .status(new TaskStatus(TaskState.FAILED))
                            .isFinal(true)
                            .build();
                    eventQueue.enqueueEvent(errorStatus);
                } finally {
                    eventQueue.close();
                }
            }, executorService);

            // 流式发送事件
            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        EventQueueItem item = eventQueue.dequeueEventItem(1000);
                        if (item != null) {
                            Event event = item.getEvent();
                            if (event instanceof StreamingEventKind) {
                                StreamingEventKind streamingEvent = (StreamingEventKind) event;
                                StreamResponse response = ToProto.streamResponse(streamingEvent);
                                responseObserver.onNext(response);

                                // 如果是最终状态，结束流
                                if (event instanceof TaskStatusUpdateEvent) {
                                    TaskStatusUpdateEvent statusEvent = (TaskStatusUpdateEvent) event;
                                    if (statusEvent.isFinal()) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    responseObserver.onCompleted();
                } catch (EventQueueClosedException e) {
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    logger.error("Error in streaming", e);
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Streaming error: " + e.getMessage())
                            .asRuntimeException());
                }
            }, executorService);

        } catch (Exception e) {
            logger.error("Error processing sendStreamingMessage", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error processing streaming message: " + e.getMessage())
                    .asRuntimeException());
        }
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

        try {
            TaskIdParams params = FromProto.taskIdParams(request);
            Task task = taskStore.get(params.id());

            if (task == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Task not found: " + params.id())
                        .asRuntimeException());
                return;
            }

            // 检查任务是否可以取消
            if (task.getStatus().state().isFinal()) {
                responseObserver.onError(Status.FAILED_PRECONDITION
                        .withDescription("Task cannot be cancelled, current state: " + task.getStatus().state())
                        .asRuntimeException());
                return;
            }

            // 创建取消后的任务
            Task cancelledTask = new Task.Builder(task)
                    .status(new TaskStatus(TaskState.CANCELED, null, OffsetDateTime.now(ZoneOffset.UTC)))
                    .build();

            taskStore.put(params.id(), cancelledTask);

            // 关闭事件队列
            queueManager.close(params.id());

            responseObserver.onNext(ToProto.task(cancelledTask));
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error processing cancelTask", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error cancelling task: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void taskSubscription(TaskSubscriptionRequest request, 
                                StreamObserver<StreamResponse> responseObserver) {
        logger.info("Received taskSubscription request for name: {}", request.getName());

        if (!agentCard.capabilities().streaming()) {
            responseObserver.onError(Status.UNIMPLEMENTED
                    .withDescription("Streaming not supported")
                    .asRuntimeException());
            return;
        }

        try {
            String taskId = request.getName();
            EventQueue queue = queueManager.tap(taskId);

            if (queue == null) {
                // 任务可能已完成，返回当前状态
                Task task = taskStore.get(taskId);
                if (task != null) {
                    StreamResponse response = ToProto.streamResponse(task);
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } else {
                    responseObserver.onError(Status.NOT_FOUND
                            .withDescription("Task not found: " + taskId)
                            .asRuntimeException());
                }
                return;
            }

            // 订阅事件队列
            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        EventQueueItem item = queue.dequeueEventItem(1000);
                        if (item != null) {
                            Event event = item.getEvent();
                            if (event instanceof StreamingEventKind) {
                                StreamingEventKind streamingEvent = (StreamingEventKind) event;
                                StreamResponse response = ToProto.streamResponse(streamingEvent);
                                responseObserver.onNext(response);

                                if (event instanceof TaskStatusUpdateEvent) {
                                    TaskStatusUpdateEvent statusEvent = (TaskStatusUpdateEvent) event;
                                    if (statusEvent.isFinal()) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    responseObserver.onCompleted();
                } catch (EventQueueClosedException e) {
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    logger.error("Error in task subscription", e);
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Subscription error: " + e.getMessage())
                            .asRuntimeException());
                } finally {
                    queue.close();
                }
            }, executorService);

        } catch (Exception e) {
            logger.error("Error processing taskSubscription", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error subscribing to task: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getAgentCard(GetAgentCardRequest request, 
                            StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
        logger.info("Received getAgentCard request");

        try {
            responseObserver.onNext(ToProto.agentCard(agentCard));
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

    /**
     * 收集事件并构建最终 Task
     */
    private Task collectEventsAndBuildTask(String taskId, String contextId, 
                                           Message userMessage, EventQueue eventQueue) {
        List<Artifact> artifacts = new ArrayList<>();
        List<Message> history = new ArrayList<>();
        history.add(userMessage);

        TaskStatus status = new TaskStatus(TaskState.SUBMITTED);

        try {
            while (true) {
                EventQueueItem item = eventQueue.dequeueEventItem(5000);
                if (item == null) {
                    continue;
                }

                Event event = item.getEvent();

                if (event instanceof Message) {
                    Message msg = (Message) event;
                    history.add(msg);
                } else if (event instanceof TaskArtifactUpdateEvent) {
                    TaskArtifactUpdateEvent artifactEvent = (TaskArtifactUpdateEvent) event;
                    artifacts.add(artifactEvent.getArtifact());
                } else if (event instanceof TaskStatusUpdateEvent) {
                    TaskStatusUpdateEvent statusEvent = (TaskStatusUpdateEvent) event;
                    status = statusEvent.getStatus();
                    if (statusEvent.isFinal()) {
                        break;
                    }
                }
            }
        } catch (EventQueueClosedException e) {
            logger.debug("Event queue closed for task: {}", taskId);
        }

        return new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(status)
                .artifacts(artifacts)
                .history(history)
                .build();
    }
}
