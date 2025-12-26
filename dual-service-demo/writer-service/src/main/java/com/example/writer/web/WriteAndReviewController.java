package com.example.writer.web;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class WriteAndReviewController {

    private final ReactAgent writerAgent;
    private final A2aRemoteAgent reviewerRemoteAgent;

    public WriteAndReviewController(ReactAgent writerAgent, A2aRemoteAgent reviewerRemoteAgent) {
        this.writerAgent = writerAgent;
        this.reviewerRemoteAgent = reviewerRemoteAgent;
    }

    @PostMapping("/write-and-review")
    public ResponseEntity<Map<String, Object>> writeAndReview(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "topic 不能为空"));
        }

        try {
            // 步骤 1: Writer Agent 生成文章
            String prompt = "请根据以下主题写一篇文章：" + topic;
            AssistantMessage articleMessage = writerAgent.call(prompt);
            String originalArticle = articleMessage.getText();

            // 步骤 2: 通过 A2A 协议调用 Reviewer Agent
            String reviewPrompt = "请对以下文章进行评审和修改：\n\n" + originalArticle;
            Optional<OverAllState> reviewState = reviewerRemoteAgent.invoke(reviewPrompt);
            String reviewedArticle = extractArticle(reviewState);

            return ResponseEntity.ok(Map.of(
                    "topic", topic,
                    "originalArticle", originalArticle,
                    "reviewedArticle", reviewedArticle,
                    "protocol", "A2A (JSON-RPC 2.0)",
                    "flow", Map.of(
                            "step1", "Writer Agent 生成文章",
                            "step2", "通过 A2A 协议调用 Reviewer Service",
                            "step3", "Reviewer Agent 评审并修改文章",
                            "step4", "返回最终结果"
                    )
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

    @SuppressWarnings("unchecked")
    private String extractArticle(Optional<OverAllState> state) {
        if (state.isEmpty()) {
            return "";
        }

        OverAllState s = state.get();

        // 尝试从 outputKey "article" 获取
        if (s.data().containsKey("article")) {
            Object articleObj = s.data().get("article");
            if (articleObj instanceof String) {
                return (String) articleObj;
            }
        }

        // 尝试从 messages 获取
        if (s.data().containsKey("messages")) {
            Object messagesObj = s.data().get("messages");
            if (messagesObj instanceof List) {
                List<Message> messages = (List<Message>) messagesObj;
                return messages.stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(msg -> (AssistantMessage) msg)
                        .reduce((first, second) -> second)
                        .map(AssistantMessage::getText)
                        .orElse("");
            }
        }

        return "";
    }
}

