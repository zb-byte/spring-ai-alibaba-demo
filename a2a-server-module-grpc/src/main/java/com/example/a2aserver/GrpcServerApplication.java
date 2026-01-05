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
public class GrpcServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GrpcServerApplication.class, args);
    }

    @Bean
    CommandLineRunner startupInfo(
            @Value("${grpc.server.port:9091}") int grpcPort,
            @Value("${agent.name:Spring AI Echo Agent}") String agentName) {
        return args -> {
            logger.info("");
            logger.info("╔══════════════════════════════════════════════════════════════╗");
            logger.info("║           A2A gRPC Server with Spring AI                     ║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Agent Name: {}", padRight(agentName, 47) + "║");
            logger.info("║  gRPC Port:  {}", padRight(String.valueOf(grpcPort), 47) + "║");
            logger.info("╚══════════════════════════════════════════════════════════════╝");
            logger.info("");
        };
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
