package com.example.a2aserver.sdk.controller;

import com.example.a2aserver.sdk.model.*;
import com.example.a2aserver.sdk.model.JSONRPCResponse;
import com.example.a2aserver.sdk.server.jsonrpc.JsonRpcProtocolServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * A2A REST controller for handling JSON-RPC requests
 */
@RestController
public class A2AController {

    private final JsonRpcProtocolServer server;
    private final ObjectMapper objectMapper;

    public A2AController(JsonRpcProtocolServer server, ObjectMapper objectMapper) {
        this.server = server;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle JSON-RPC requests
     */
    @PostMapping(
            path = "/a2a",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JSONRPCResponse> handleJsonRpcRequest(@RequestBody JSONRPCRequest request) {

        if (!"2.0".equals(request.getJsonrpc())) {
            JSONRPCError error = new JSONRPCError(
                    ErrorCode.INVALID_REQUEST.getValue(),
                    "Invalid JSON-RPC version",
                    null
            );
            JSONRPCResponse response = new JSONRPCResponse(
                    request.getId(),
                    "2.0",
                    null,
                    error
            );
            return ResponseEntity.badRequest().body(response);
        }

        JSONRPCResponse response = switch (request.getMethod()) {
            case "message/send" -> server.handleTaskSend(request);
            case "tasks/get" -> server.handleTaskGet(request);
            case "tasks/cancel" -> server.handleTaskCancel(request);
            default -> {
                JSONRPCError error = new JSONRPCError(
                        ErrorCode.METHOD_NOT_FOUND.getValue(),
                        "Method not found",
                        null
                );
                yield new JSONRPCResponse(
                        request.getId(),
                        "2.0",
                        null,
                        error
                );
            }
        };

        return ResponseEntity.ok(response);
    }

    /**
     * Handle streaming task requests (Server-Sent Events)
     */
    @PostMapping(
            value = "/a2a/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter handleStreamingTask(@RequestBody JSONRPCRequest request) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Process task asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                if (!"message/send".equals(request.getMethod())) {
                    sendErrorEvent(emitter, request.getId(), ErrorCode.METHOD_NOT_FOUND, "Method not found");
                    return;
                }

                TaskSendParams params = parseTaskSendParams(request.getParams());

                // Create initial status with timestamp
                TaskStatus initialStatus = new TaskStatus(
                    TaskState.WORKING
                );

                // Send initial status update
                //String taskId, TaskStatus status, String contextId, boolean isFinal,
                //                                 Map<String, Object> metadata
                TaskStatusUpdateEvent initialEvent = new TaskStatusUpdateEvent(
                        params.id(),
                        initialStatus,"",
                        false,  // final
                        null    // metadata
                );

                SendTaskStreamingResponse initialResponse = new SendTaskStreamingResponse(
                        request.getId(),
                        "2.0",
                        initialEvent,
                        null
                );

                emitter.send(SseEmitter.event()
                        .name("task-update")
                        .data(objectMapper.writeValueAsString(initialResponse)));

                // Process task
                JSONRPCResponse taskResponse = server.handleTaskSend(request);

                if (taskResponse.error() != null) {
                    sendErrorEvent(emitter, request.getId(), ErrorCode.INTERNAL_ERROR, taskResponse.error().getMessage());
                    return;
                }

                // Send final status update
                Task completedTask = (Task) taskResponse.result();
                TaskStatusUpdateEvent finalEvent = new TaskStatusUpdateEvent(
                        completedTask.getId(),
                        completedTask.getStatus(),
                        "",
                        true,   // final
                        null    // metadata
                );

                SendTaskStreamingResponse finalResponse = new SendTaskStreamingResponse(
                        request.getId(),
                        "2.0",
                        finalEvent,
                        null
                );

                emitter.send(SseEmitter.event()
                        .name("task-update")
                        .data(objectMapper.writeValueAsString(finalResponse)));

                emitter.complete();

            } catch (Exception e) {
                sendErrorEvent(emitter, request.getId(), ErrorCode.INTERNAL_ERROR, e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * Get agent card information
     */
    @GetMapping("/.well-known/agent-card.json")
    public ResponseEntity<AgentCard> getAgentCard() {
        return ResponseEntity.ok(server.getAgentCard());
    }

    /**
     * Parse TaskSendParams
     */
    private TaskSendParams parseTaskSendParams(Object params) throws Exception {
        return objectMapper.convertValue(params, TaskSendParams.class);
    }

    /**
     * Send error event
     */
    private void sendErrorEvent(SseEmitter emitter, Object requestId, ErrorCode code, String message) {
        try {
            XA2AError error = new XA2AError(code, message, null);
            SendTaskStreamingResponse errorResponse = new SendTaskStreamingResponse(
                    requestId,
                    "2.0",
                    null,
                    error
            );

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(errorResponse)));

            emitter.completeWithError(new RuntimeException(message));

        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
