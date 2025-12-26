package com.example.aidemo.demo;

import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.a2a.spec.AgentCard;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * A2A 客户端流式响应演示
 * 
 * 本类提供了流式响应的 HTTP 端点，演示如何使用 Spring AI Alibaba 的 A2aRemoteAgent
 * 进行流式通信。支持三种传输协议：JSON-RPC 2.0、gRPC、HTTP+JSON/REST
 * 
 * 与 TransportProtocolDemoController 的区别：
 * - TransportProtocolDemoController: 提供非流式的 HTTP API（等待完整响应后返回）
 * - A2aClientDemo: 提供流式响应的 HTTP API（Server-Sent Events，实时返回数据流）
 * 
 * @author spring-ai-alibaba-demo
 */
@RestController
@RequestMapping("/demo/streaming")
public class A2aClientDemo {

    private final A2aRemoteAgent remoteAgent;

    public A2aClientDemo(A2aRemoteAgent remoteAgent, AgentCard agentCard) {
        this.remoteAgent = remoteAgent;
    }

    /**
     * 流式消息发送端点（Server-Sent Events）
     * 所有三种协议都支持流式通信
     * 
     * 使用示例：
     * curl -N -H "Accept: text/event-stream" \
     *   -X POST http://localhost:8080/demo/streaming/a2a \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "请详细介绍一下 Spring AI Alibaba"}'
     */
    @PostMapping(value = "/a2a", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("message 不能为空"));
        }

        try {
            UserMessage userMessage = new UserMessage(message);
            
            return remoteAgent.stream(userMessage, null)
                .filter(nodeOutput -> nodeOutput instanceof StreamingOutput)
                .map(nodeOutput -> (StreamingOutput<?>) nodeOutput)
                .map(streamingOutput -> {
                    Object chunk = streamingOutput.chunk();
                    return chunk != null ? chunk.toString() : "";
                })
                .filter(chunk -> !chunk.isEmpty())
                .onErrorResume(error -> Flux.just("Error: " + error.getMessage()));
        } catch (Exception e) {
            return Flux.error(e);
        }
    }
}
