package com.example.a2aclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 简化的 A2A gRPC Client Demo
 * 
 * 基于 A2A Java SDK v0.3.3.Final 版本
 * 由于 API 复杂性，这里提供一个基础框架和连接测试
 */
@Component
public class SimpleA2AClientDemo implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SimpleA2AClientDemo.class);

    @Value("${a2a.server.host:localhost}")
    private String serverHost;

    @Value("${a2a.server.port:7002}")
    private int serverPort;

    @Value("${a2a.server.grpc-port:9090}")
    private int grpcPort;

    @Value("${a2a.server.agent-card-url}")
    private String agentCardUrl;

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== A2A gRPC Client Demo ===");
        logger.info("基于 A2A Java SDK v0.3.3.Final");
        logger.info("");
        logger.info("🎯 目标服务器配置:");
        logger.info("   - Host: {}", serverHost);
        logger.info("   - HTTP Port: {}", serverPort);
        logger.info("   - gRPC Port: {}", grpcPort);
        logger.info("   - Agent Card URL: {}", agentCardUrl);
        logger.info("");

        // 测试服务器连接
        testServerConnection();

        logger.info("");
        logger.info("📋 已包含的 A2A SDK 组件:");
        logger.info("   - a2a-java-sdk-client (客户端核心)");
        logger.info("   - a2a-java-sdk-client-transport-grpc (gRPC 客户端传输)");
        logger.info("   - a2a-java-sdk-spec (协议规范)");
        logger.info("   - a2a-java-sdk-spec-grpc (gRPC 协议绑定)");
        logger.info("   - a2a-java-sdk-http-client (HTTP 客户端)");
        logger.info("");
        logger.info("🚀 下一步开发建议:");
        logger.info("   1. 研究 A2A Client SDK 0.3.3.Final 的实际 API");
        logger.info("   2. 实现 AgentCard 获取和解析");
        logger.info("   3. 配置 gRPC 客户端连接");
        logger.info("   4. 实现消息发送和接收逻辑");
        logger.info("   5. 添加 Web 界面进行交互测试");
        logger.info("");
        logger.info("📚 参考资源:");
        logger.info("   - A2A Protocol: https://a2a-protocol.org/");
        logger.info("   - A2A Java SDK: https://github.com/a2aproject/a2a-java");
        logger.info("   - Maven Repository: https://mvnrepository.com/artifact/io.github.a2asdk");
        logger.info("");
        logger.info("🌐 Web 界面: http://localhost:7001/");
    }

    private void testServerConnection() {
        logger.info("🔍 测试服务器连接...");
        
        try {
            // 测试 Agent Card 端点
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentCardUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("✅ Agent Card 端点连接成功");
                logger.info("📄 Agent Card 内容:");
                
                // 简单格式化 JSON 输出
                String jsonContent = response.body();
                String[] lines = jsonContent.split(",");
                for (String line : lines) {
                    logger.info("   {}", line.trim());
                }
            } else {
                logger.warn("⚠️ Agent Card 端点返回状态码: {}", response.statusCode());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("❌ 无法连接到服务器: {}", e.getMessage());
            logger.info("💡 请确保 A2A Server 正在运行:");
            logger.info("   cd ../a2a-server-module-grpc && mvn spring-boot:run");
            
            // 恢复中断状态
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 测试 gRPC 端口连通性
        testGrpcConnection();
    }

    private void testGrpcConnection() {
        try (java.net.Socket socket = new java.net.Socket()) {
            // 简单的端口连通性测试
            socket.connect(new InetSocketAddress(serverHost, grpcPort), 3000);
            logger.info("✅ gRPC 端口 {} 连接成功", grpcPort);
        } catch (IOException e) {
            logger.warn("⚠️ gRPC 端口 {} 连接失败: {}", grpcPort, e.getMessage());
            logger.info("💡 gRPC 服务器可能尚未实现或未启动");
        }
    }
}