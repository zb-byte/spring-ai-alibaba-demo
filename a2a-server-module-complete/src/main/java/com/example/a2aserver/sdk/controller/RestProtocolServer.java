package com.example.a2aserver.sdk.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 协议服务器实现
 *
 * 直接实现 REST 端点，不依赖 A2A SDK 的 RequestHandler
 */
@RestController
@RequestMapping("/rest")
public class RestProtocolServer  {

    private final Map<String, Object> taskStore = new HashMap<>();
    /**
     * 获取 Agent 卡片
     */
    @GetMapping(value = "/.well-known/agent-card.json", produces = "application/json")
    public Object getAgentCard() {
        try {
            return null;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 发送消息（同步）
     */
    @PostMapping(value = "/v1/message:send", consumes = "application/json", produces = "application/json")
    public Object sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String taskId = UUID.randomUUID().toString();
            String message = extractMessage(request);
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            return result;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping(value = "/v1/tasks/{taskId}", produces = "application/json")
    public Object getTask(@PathVariable String taskId,
                         @RequestParam(defaultValue = "0") int historyLength) {
        try {
            Object task = taskStore.get(taskId);
            if (task == null) {
                return Map.of("error", "Task not found: " + taskId);
            }
            return task;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    @PostMapping(value = "/v1/tasks/{taskId}:cancel", produces = "application/json")
    public Object cancelTask(@PathVariable String taskId) {
        try {
            // TODO: 实现任务取消逻辑
            return Map.of(
                "taskId", taskId,
                "status", "canceled"
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    private String extractMessage(Map<String, Object> request) {
        // 尝试从不同的位置提取消息
        if (request.containsKey("message")) {
            return request.get("message").toString();
        }
        if (request.containsKey("content")) {
            return request.get("content").toString();
        }
        if (request.containsKey("params")) {
            Object params = request.get("params");
            if (params instanceof Map) {
                Map<?, ?> paramMap = (Map<?, ?>) params;
                if (paramMap.containsKey("message")) {
                    return paramMap.get("message").toString();
                }
            }
        }
        return request.toString();
    }
}
