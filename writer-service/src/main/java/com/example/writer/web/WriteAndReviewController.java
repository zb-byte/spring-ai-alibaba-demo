package com.example.writer.web;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.example.writer.service.PlannerAgentService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WriteAndReviewController {

    private final ReactAgent plannerAgent;
    private final PlannerAgentService plannerAgentService;

    public WriteAndReviewController(ReactAgent plannerAgent, PlannerAgentService plannerAgentService) {
        this.plannerAgent = plannerAgent;
        this.plannerAgentService = plannerAgentService;
    }

    /**
     * 调用 Planner Agent（调度大脑）的接口
     * Planner Agent 会：
     * 1. 发现 Agent - 发现所有可用的 Agent
     * 2. 理解 Agent 能力 - 理解每个 Agent 能做什么
     * 3. 选择 & 调用 - 根据需求选择合适的 Agent 并调用
     */
    @PostMapping("/planner/invoke")
    public ResponseEntity<Map<String, Object>> invokePlannerAgent(@RequestBody Map<String, String> request) {
        String userRequest = request.get("request") != null ? request.get("request") : request.get("topic");
        if (userRequest == null || userRequest.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "request 或 topic 不能为空"));
        }

        try {
            AssistantMessage message = plannerAgent.call(userRequest);
            String result = message.getText();
            
            return ResponseEntity.ok(Map.of(
                    "request", userRequest,
                    "result", result,
                    "agent", "planner-agent",
                    "description", "Planner Agent 作为调度大脑，自动发现、理解并调用合适的 Agent 完成任务"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 发现所有可用的 Agent（用于测试）
     */
    @GetMapping("/agents/discover")
    public ResponseEntity<Map<String, Object>> discoverAgents() {
        try {
            String agents = plannerAgentService.discoverAgents();
            return ResponseEntity.ok(Map.of(
                    "agents", agents,
                    "description", "Planner Agent 发现的所有可用 Agent"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "writer-service"));
    }
}

