package com.example.a2aclient.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.a2aclient.client.A2ARestClient;

import io.a2a.spec.AgentCard;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

/**
 * 测试 Controller
 * 提供 REST 端点用于测试 A2A Client (v0.3.3.Final)
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    private final A2ARestClient a2aClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TestController(A2ARestClient a2aClient) {
        this.a2aClient = a2aClient;
    }

    @GetMapping(value = "/agent-card", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAgentCard() {
        try {
            AgentCard card = a2aClient.fetchAgentCard();
            Map<String, Object> result = new HashMap<>();
            result.put("name", card.name());
            result.put("description", card.description());
            result.put("version", card.version());
            result.put("url", card.url());
            result.put("protocolVersion", card.protocolVersion());
            result.put("capabilities", Map.of(
                    "streaming", card.capabilities().streaming(),
                    "pushNotifications", card.capabilities().pushNotifications(),
                    "stateTransitionHistory", card.capabilities().stateTransitionHistory()
            ));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to fetch agent card", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestParam String msg) {
        try {
            logger.info("Test: sending message: {}", msg);
            EventKind result = a2aClient.sendMessage(msg);
            
            Map<String, Object> response = new HashMap<>();
            if (result instanceof Message message) {
                response.put("type", "message");
                response.put("messageId", message.getMessageId());
                response.put("role", message.getRole().asString());
                response.put("text", extractText(message));
            } else if (result instanceof Task task) {
                response.put("type", "task");
                response.put("taskId", task.getId());
                response.put("status", task.getStatus().state().asString());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStreaming(@RequestParam String msg) {
        SseEmitter emitter = new SseEmitter(60000L);
        
        executor.submit(() -> {
            try {
                logger.info("Test: sending streaming message: {}", msg);
                List<Map<String, Object>> events = new ArrayList<>();
                
                a2aClient.sendMessageStreaming(msg, event -> {
                    try {
                        Map<String, Object> eventData = new HashMap<>();
                        if (event instanceof Message message) {
                            eventData.put("type", "message");
                            eventData.put("messageId", message.getMessageId());
                            eventData.put("role", message.getRole().asString());
                            eventData.put("text", extractText(message));
                        } else if (event instanceof Task task) {
                            eventData.put("type", "task");
                            eventData.put("taskId", task.getId());
                            eventData.put("status", task.getStatus().state().asString());
                        }
                        events.add(eventData);
                        emitter.send(SseEmitter.event().data(eventData));
                    } catch (Exception e) {
                        logger.error("Error sending SSE event", e);
                    }
                });
                
                emitter.complete();
            } catch (Exception e) {
                logger.error("Streaming error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    @GetMapping(value = "/task/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        try {
            Task task = a2aClient.getTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("contextId", task.getContextId());
            result.put("status", task.getStatus().state().asString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get task", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractText(Message message) {
        if (message.getParts() == null || message.getParts().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : message.getParts()) {
            if (part instanceof TextPart textPart) {
                sb.append(textPart.getText());
            }
        }
        return sb.toString();
    }
}
