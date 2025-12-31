package com.example.a2aserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A2A gRPC Server 主应用
 * 
 * 基于 Spring Boot + Spring AI + A2A Java SDK 构建的 Agent 服务器
 */
@SpringBootApplication
public class A2aServerModuleApplication {

    private static final Logger logger = LoggerFactory.getLogger(A2aServerModuleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(A2aServerModuleApplication.class, args);
    }

    @Bean
    CommandLineRunner startupInfo(
            @Value("${server.port:7002}") int httpPort,
            @Value("${grpc.server.port:9090}") int grpcPort,
            @Value("${agent.name:Spring AI Echo Agent}") String agentName) {
        return args -> {
            logger.info("");
            logger.info("╔══════════════════════════════════════════════════════════════╗");
            logger.info("║           A2A gRPC Server with Spring AI                     ║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Agent Name: {}", padRight(agentName, 47) + "║");
            logger.info("║  HTTP Port:  {}", padRight(String.valueOf(httpPort), 47) + "║");
            logger.info("║  gRPC Port:  {}", padRight(String.valueOf(grpcPort), 47) + "║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Endpoints:                                                  ║");
            logger.info("║    - Web UI:     http://localhost:{}/", padRight(String.valueOf(httpPort), 36) + "║");
            logger.info("║    - Agent Card: http://localhost:{}/.well-known/agent-card.json ║", httpPort);
            logger.info("║    - gRPC:       grpc://localhost:{}", padRight(String.valueOf(grpcPort), 36) + "║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Capabilities:                                               ║");
            logger.info("║    ✅ Streaming          ✅ State History                    ║");
            logger.info("║    ❌ Push Notifications                                     ║");
            logger.info("╚══════════════════════════════════════════════════════════════╝");
            logger.info("");
        };
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
