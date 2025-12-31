package com.example.a2aserver.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Card HTTP 端点
 * 提供标准的 /.well-known/agent-card.json 端点，用于 Agent 发现
 */
@RestController
public class AgentCardController {

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAgentCard() {
        // 返回一个符合 A2A 协议的 AgentCard JSON
        return """
        {
          "name": "Echo Agent",
          "description": "A simple Echo Agent for A2A protocol demo",
          "version": "1.0.0",
          "documentationUrl": "https://github.com/example/a2a-echo-agent",
          "capabilities": {
            "streaming": true,
            "pushNotifications": false,
            "stateTransitionHistory": false,
            "extensions": []
          },
          "defaultInputModes": ["text"],
          "defaultOutputModes": ["text"],
          "skills": [{
            "id": "echo",
            "name": "Echo Message",
            "description": "Echoes back the received message",
            "tags": ["echo", "test"],
            "examples": ["Hello", "Test message"],
            "inputModes": [],
            "outputModes": [],
            "security": []
          }],
          "supportsExtendedAgentCard": false,
          "supportedInterfaces": [{
            "protocol": "grpc",
            "url": "grpc://localhost:9090"
          }],
          "protocolVersion": "1.0"
        }
        """;
    }

    @GetMapping(value = "/agent-card", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAgentCardAlternative() {
        return getAgentCard();
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String getIndex() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>A2A Echo Agent</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 40px; }
                .container { max-width: 800px; }
                .status { color: green; font-weight: bold; }
                .endpoint { background: #f5f5f5; padding: 10px; border-radius: 5px; margin: 10px 0; }
                .code { font-family: monospace; background: #f0f0f0; padding: 2px 4px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🤖 A2A Echo Agent</h1>
                <p class="status">✅ Server is running</p>
                
                <h2>📋 Agent Information</h2>
                <ul>
                    <li><strong>Name:</strong> Echo Agent</li>
                    <li><strong>Version:</strong> 1.0.0</li>
                    <li><strong>Protocol:</strong> A2A v1.0</li>
                    <li><strong>Transport:</strong> gRPC</li>
                </ul>
                
                <h2>🔗 Endpoints</h2>
                <div class="endpoint">
                    <strong>Agent Card Discovery:</strong><br>
                    <a href="/.well-known/agent-card.json" target="_blank">
                        <span class="code">GET /.well-known/agent-card.json</span>
                    </a>
                </div>
                
                <div class="endpoint">
                    <strong>gRPC Service:</strong><br>
                    <span class="code">grpc://localhost:9090</span>
                </div>
                
                <h2>🛠️ Development Status</h2>
                <ul>
                    <li>✅ Maven 依赖配置 (A2A SDK v0.3.3.Final)</li>
                    <li>✅ Spring Boot 集成</li>
                    <li>✅ Agent Card 端点</li>
                    <li>⏳ gRPC 服务实现 (待开发)</li>
                    <li>⏳ Agent 业务逻辑 (待开发)</li>
                </ul>
                
                <h2>📚 Resources</h2>
                <ul>
                    <li><a href="https://a2a-protocol.org/" target="_blank">A2A Protocol Specification</a></li>
                    <li><a href="https://github.com/a2aproject/a2a-java" target="_blank">A2A Java SDK</a></li>
                    <li><a href="https://mvnrepository.com/artifact/io.github.a2asdk" target="_blank">Maven Repository</a></li>
                </ul>
            </div>
        </body>
        </html>
        """;
    }
}