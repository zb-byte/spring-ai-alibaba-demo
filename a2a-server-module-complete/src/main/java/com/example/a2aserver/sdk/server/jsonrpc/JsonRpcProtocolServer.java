package com.example.a2aserver.sdk.server.jsonrpc;

import java.util.Map;
import java.util.UUID;

import com.example.a2aserver.sdk.model.ErrorCode;
import com.example.a2aserver.sdk.model.TaskSendParams;
import com.example.a2aserver.sdk.model.JSONRPCResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class JsonRpcProtocolServer   {
    private final AgentCard agentCard;
    private final TaskHandler handler;
    private final Map<String, Task> taskStore;
    private final Map<String, List<Message>> taskHistory;
    private final ObjectMapper objectMapper;

    public JsonRpcProtocolServer(AgentCard agentCard, TaskHandler handler, ObjectMapper objectMapper) {
        this.agentCard = agentCard;
        this.handler = handler;
        this.taskStore = new ConcurrentHashMap<>();
        this.taskHistory = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
    }

    /**
     * Handle task send request
     */
    public JSONRPCResponse handleTaskSend(JSONRPCRequest<TaskSendParams> request) {
        try {
            TaskSendParams params = request.getParams();

            // Generate contextId if not provided
            String contextId = UUID.randomUUID().toString();

            // Create initial task status
            TaskStatus initialStatus = new TaskStatus(
                    TaskState.WORKING// Current timestamp
            );

            // Create new task with all required fields
            Task task = new Task(
                    params.id(),
                    contextId,
                    initialStatus,
                    null,    // No artifacts initially
                    null,    // No history initially
                    params.metadata()  // Use metadata from params
            );

            // Process task
            Task updatedTask = handler.handle(task, params.message());

            // Store task and history
            taskStore.put(task.getId(), updatedTask);
            taskHistory.computeIfAbsent(task.getId(), k -> new CopyOnWriteArrayList<>())
                    .add(params.message());

            return createSuccessResponse(request.getId(), updatedTask);

        } catch (Exception e) {
            return createErrorResponse(request.getId(), ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Handle task query request
     */
    public JSONRPCResponse handleTaskGet(JSONRPCRequest<TaskQueryParams> request) {
        try {
            TaskQueryParams params = request.getParams();

            Task task = taskStore.get(params.id());
            if (task == null) {
                return createErrorResponse(request.getId(), ErrorCode.TASK_NOT_FOUND, "Task not found");
            }

            // Include history if requested
            if (params.historyLength() > 0) {
                List<Message> history = getTaskHistory(params.id());
                int limit = Math.min(params.historyLength(), history.size());
                List<Message> limitedHistory = history.subList(Math.max(0, history.size() - limit), history.size());

                // Create task with history
                Task taskWithHistory = new Task(
                        task.getId(),
                        task.getContextId(),
                        task.getStatus(),
                        task.getArtifacts(),
                        limitedHistory,
                        task.getMetadata()
                );

                return createSuccessResponse(request.getId(), taskWithHistory);
            }

            return createSuccessResponse(request.getId(), task);

        } catch (Exception e) {
            return createErrorResponse(request.getId(), ErrorCode.INVALID_REQUEST, "Invalid parameters");
        }
    }

    /**
     * Handle task cancel request
     */
    public JSONRPCResponse handleTaskCancel(JSONRPCRequest<TaskIdParams> request) {
        try {
            TaskIdParams params = request.getParams();

            Task task = taskStore.get(params.id());
            if (task == null) {
                return createErrorResponse(request.getId(), ErrorCode.TASK_NOT_FOUND, "Task not found");
            }

            // Check if task can be canceled
            if (task.getStatus().state() == TaskState.COMPLETED ||
                    task.getStatus().state() == TaskState.CANCELED ||
                    task.getStatus().state() == TaskState.FAILED) {
                return createErrorResponse(request.getId(), ErrorCode.TASK_NOT_CANCELABLE, "Task cannot be canceled");
            }

            // Create canceled status with timestamp
            TaskStatus canceledStatus = new TaskStatus(
                    TaskState.CANCELED
            );

            // Update task status to canceled
            Task canceledTask = new Task(
                    task.getId(),
                    task.getContextId(),
                    canceledStatus,
                    task.getArtifacts(),
                    task.getHistory(),
                    task.getMetadata()
            );

            taskStore.put(params.id(), canceledTask);

            return createSuccessResponse(request.getId(), canceledTask);

        } catch (Exception e) {
            return createErrorResponse(request.getId(), ErrorCode.INVALID_REQUEST, "Invalid parameters");
        }
    }

    /**
     * Get agent card information
     */
    public AgentCard getAgentCard() {
        return agentCard;
    }

    /**
     * Get task history
     */
    public List<Message> getTaskHistory(String taskId) {
        return taskHistory.getOrDefault(taskId, List.of());
    }

    /**
     * Parse request parameters
     */
    private <T> T parseParams(Object params, Class<T> clazz) throws Exception {
        return objectMapper.convertValue(params, clazz);
    }

    /**
     * Create success response
     */
    private JSONRPCResponse createSuccessResponse(Object id, Object result) {
        return new JSONRPCResponse(
                id,
                "2.0",
                result,
                null
        );
    }

    /**
     * Create error response
     */
    private JSONRPCResponse createErrorResponse(Object id, ErrorCode code, String message) {
        JSONRPCError error = new JSONRPCError(code.getValue(), message, null);
        return new JSONRPCResponse(id, "2.0", "error",error);
    }
}
