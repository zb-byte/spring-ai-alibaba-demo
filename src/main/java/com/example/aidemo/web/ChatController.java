package com.example.aidemo.web;

import com.example.aidemo.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) throws Exception {
        String answer = agentService.chatViaA2a(request.question());
        return ResponseEntity.ok(new ChatResponse("a2a", answer));
    }

    @PostMapping("/local")
    public ResponseEntity<ChatResponse> chatLocal(@RequestBody ChatRequest request) throws Exception {
        String answer = agentService.chatLocal(request.question());
        return ResponseEntity.ok(new ChatResponse("local", answer));
    }

    @PostMapping("/a2a")
    public ResponseEntity<ChatResponse> chatA2a(@RequestBody ChatRequest request) throws Exception {
        String answer = agentService.chatViaA2a(request.question());
        return ResponseEntity.ok(new ChatResponse("a2a", answer));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleException(Exception exception) {
        log.error("LLM 调用失败", exception);
        return ResponseEntity.internalServerError()
                .body(new ChatResponse("error", "LLM 调用失败: " + exception.getMessage()));
    }

    public record ChatRequest(String question) {
    }

    public record ChatResponse(String mode, String answer) {
    }
}

