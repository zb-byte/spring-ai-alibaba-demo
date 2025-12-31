package com.example.a2aclient.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.a2aclient.A2AClientService;

/**
 * A2A Client Demo Web 控制器
 * 提供 Web 界面进行 A2A 客户端测试
 */
@RestController
@RequestMapping("/api")
public class ClientDemoController {

    private static final Logger logger = LoggerFactory.getLogger(ClientDemoController.class);

    @Autowired
    private A2AClientService a2aClientService;

    @Value("${a2a.server.host:localhost}")
    private String serverHost;

    @Value("${a2a.server.port:7002}")
    private int serverPort;

    @Value("${a2a.server.grpc-port:9090}")
    private int grpcPort;

    @Value("${a2a.server.agent-card-url}")
    private String agentCardUrl;

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试 HTTP 连接
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentCardUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            result.put("status", "success");
            result.put("httpStatus", response.statusCode());
            result.put("serverHost", serverHost);
            result.put("serverPort", serverPort);
            result.put("grpcPort", grpcPort);
            result.put("message", "服务器连接成功");
            
            if (response.statusCode() == 200) {
                result.put("agentCardAvailable", true);
            } else {
                result.put("agentCardAvailable", false);
                result.put("warning", "Agent Card 端点返回状态码: " + response.statusCode());
            }
            
        } catch (Exception e) {
            logger.error("连接测试失败", e);
            result.put("status", "error");
            result.put("message", "连接失败: " + e.getMessage());
            result.put("suggestion", "请确保 A2A Server 正在运行");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/agent-card")
    public ResponseEntity<Map<String, Object>> getAgentCard() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentCardUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                result.put("status", "success");
                result.put("agentCard", response.body());
                result.put("message", "Agent Card 获取成功");
            } else {
                result.put("status", "error");
                result.put("message", "Agent Card 获取失败，状态码: " + response.statusCode());
            }
            
        } catch (Exception e) {
            logger.error("Agent Card 获取失败", e);
            result.put("status", "error");
            result.put("message", "Agent Card 获取失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send-message")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendMessage(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String message = request.get("message");
        
        logger.info("收到消息发送请求: {}", message);
        
        // 使用 A2A 客户端服务发送消息
        return a2aClientService.sendMessage(message)
            .thenApply(response -> {
                result.put("status", "success");
                result.put("originalMessage", message);
                result.put("response", response);
                result.put("timestamp", System.currentTimeMillis());
                result.put("method", "A2A gRPC Client");
                
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                logger.error("消息发送失败", throwable);
                result.put("status", "error");
                result.put("originalMessage", message);
                result.put("error", throwable.getMessage());
                result.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(result);
            });
    }

    @GetMapping("/send-message-a2a")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendMessageViaA2A(@RequestParam String message) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("通过 A2A SDK 发送消息: {}", message);
        
        // 使用 A2A 客户端服务发送消息
        return a2aClientService.sendMessage(message)
            .thenApply(response -> {
                result.put("status", "success");
                result.put("originalMessage", message);
                result.put("a2aResponse", response);
                result.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                logger.error("A2A 消息发送失败", throwable);
                result.put("status", "error");
                result.put("originalMessage", message);
                result.put("error", throwable.getMessage());
                result.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(result);
            });
    }

    @GetMapping("/test-grpc")
    public ResponseEntity<Map<String, Object>> testGrpcConnection() {
        Map<String, Object> result = new HashMap<>();
        
        // 使用 A2A 客户端服务测试连接
        boolean connected = a2aClientService.testConnection();
        
        if (connected) {
            result.put("status", "success");
            result.put("message", "A2A gRPC 连接成功");
            result.put("grpcHost", serverHost);
            result.put("grpcPort", grpcPort);
        } else {
            result.put("status", "warning");
            result.put("message", "A2A gRPC 连接失败");
            result.put("suggestion", "请确保 A2A gRPC 服务器正在运行");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/agent-card-a2a")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAgentCardViaA2A() {
        Map<String, Object> result = new HashMap<>();
        
        // 使用 A2A 客户端服务获取 Agent Card
        return a2aClientService.getAgentCard()
            .thenApply(agentCard -> {
                result.put("status", "success");
                result.put("agentCard", agentCard);
                result.put("message", "通过 A2A SDK 获取 Agent Card 成功");
                result.put("method", "A2A gRPC Client");
                
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                logger.error("A2A Agent Card 获取失败", throwable);
                result.put("status", "error");
                result.put("message", "A2A Agent Card 获取失败: " + throwable.getMessage());
                
                return ResponseEntity.ok(result);
            });
    }
}