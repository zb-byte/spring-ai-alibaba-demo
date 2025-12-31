package com.example.a2aserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.a2a.spec.AgentCard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Card HTTP 端点
 * 提供标准的 /.well-known/agent-card.json 端点，用于 Agent 发现
 */
@RestController
public class AgentCardController {

    private final AgentCard agentCard;
    private final ObjectMapper objectMapper;

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    public AgentCardController(AgentCard agentCard) {
        this.agentCard = agentCard;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAgentCard() {
        try {
            return objectMapper.writeValueAsString(agentCard);
        } catch (Exception e) {
            return getAgentCardFallback();
        }
    }

    @GetMapping(value = "/agent-card", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAgentCardAlternative() {
        return getAgentCard();
    }

    private String getAgentCardFallback() {
        return """
        {
          "name": "%s",
          "description": "%s",
          "version": "1.0.0",
          "capabilities": {
            "streaming": true,
            "pushNotifications": false,
            "stateTransitionHistory": true
          },
          "defaultInputModes": ["text"],
          "defaultOutputModes": ["text"],
          "skills": [{
            "id": "chat",
            "name": "Chat",
            "description": "General conversation powered by Spring AI"
          }],
          "supportedInterfaces": [{
            "protocol": "grpc",
            "url": "grpc://localhost:%d"
          }],
          "protocolVersion": "1.0"
        }
        """.formatted(agentCard.name(), agentCard.description(), grpcPort);
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String getIndex() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>A2A Spring AI Agent</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                .container { max-width: 900px; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .status { color: green; font-weight: bold; }
                .endpoint { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0; border-left: 4px solid #007bff; }
                .code { font-family: monospace; background: #f0f0f0; padding: 2px 6px; border-radius: 3px; }
                .feature { display: inline-block; background: #28a745; color: white; padding: 3px 8px; border-radius: 3px; margin: 2px; font-size: 12px; }
                .feature-disabled { background: #6c757d; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🤖 A2A Spring AI Agent</h1>
                <p class="status">✅ Server is running</p>
                
                <h2>📋 Agent Information</h2>
                <ul>
                    <li><strong>Name:</strong> %s</li>
                    <li><strong>Version:</strong> 1.0.0</li>
                    <li><strong>Protocol:</strong> A2A v1.0</li>
                    <li><strong>Transport:</strong> gRPC + HTTP</li>
                    <li><strong>AI Backend:</strong> Spring AI (OpenAI compatible)</li>
                </ul>
                
                <h2>🎯 Capabilities</h2>
                <div>
                    <span class="feature">✅ Streaming</span>
                    <span class="feature feature-disabled">❌ Push Notifications</span>
                    <span class="feature">✅ State History</span>
                </div>
                
                <h2>🔗 Endpoints</h2>
                <div class="endpoint">
                    <strong>Agent Card Discovery:</strong><br>
                    <a href="/.well-known/agent-card.json" target="_blank">
                        <span class="code">GET /.well-known/agent-card.json</span>
                    </a>
                </div>
                
                <div class="endpoint">
                    <strong>gRPC Service:</strong><br>
                    <span class="code">grpc://localhost:%d</span>
                </div>
                
                <h2>🛠️ Skills</h2>
                <ul>
                    <li><strong>chat</strong> - General conversation and Q&A powered by Spring AI</li>
                    <li><strong>echo</strong> - Echoes back the received message (fallback mode)</li>
                </ul>
                
                <h2>📚 Usage</h2>
                <div class="endpoint">
                    <strong>gRPC Client Example:</strong><br>
                    <pre><code>// 使用 A2A Java SDK Client
A2AClient client = A2AClient.builder()
    .transport(GrpcTransport.builder()
        .host("localhost")
        .port(%d)
        .build())
    .build();

// 发送消息
Message message = Message.builder()
    .role(Message.Role.USER)
    .parts(List.of(new TextPart("Hello, AI!")))
    .build();

Task task = client.sendMessage(message);</code></pre>
                </div>
                
                <h2>📖 Resources</h2>
                <ul>
                    <li><a href="https://a2a-protocol.org/" target="_blank">A2A Protocol Specification</a></li>
                    <li><a href="https://github.com/a2aproject/a2a-java" target="_blank">A2A Java SDK</a></li>
                    <li><a href="https://spring.io/projects/spring-ai" target="_blank">Spring AI</a></li>
                </ul>
            </div>
        </body>
        </html>
        """.formatted(agentCard.name(), grpcPort, grpcPort);
    }
}
