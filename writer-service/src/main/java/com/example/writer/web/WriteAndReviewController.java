package com.example.writer.web;

import com.example.writer.service.WriteAndReviewService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WriteAndReviewController {

    private final WriteAndReviewService writeAndReviewService;

    public WriteAndReviewController(WriteAndReviewService writeAndReviewService) {
        this.writeAndReviewService = writeAndReviewService;
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
        String topic = request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "request 或 topic 不能为空"));
        }

        try {
            String  message = writeAndReviewService.writeAndReview(topic);            
            return ResponseEntity.ok(Map.of(
                    "result", message
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

