package com.example.a2aserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A2A HTTP REST Server Demo
 * 基于 A2A Java SDK v0.3.3.Final 构建的最小可运行 REST Server
 */
@SpringBootApplication
public class RestServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(RestServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RestServerApplication.class, args);
    }

    @Bean
    CommandLineRunner startupInfo(@Value("${server.port:7002}") int httpPort) {
        return args -> {
            logger.info("");
            logger.info("╔══════════════════════════════════════════════════════════════╗");
            logger.info("║           A2A REST Server (SDK v0.3.3.Final)                ║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  HTTP Port: {}                                              ║", httpPort);
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Endpoints:                                                  ║");
            logger.info("║    GET  /.well-known/agent-card.json  - Agent Card          ║");
            logger.info("║    POST /message:send                 - Send Message        ║");
            logger.info("║    POST /message:stream               - Streaming Message   ║");
            logger.info("║    GET  /tasks/{taskId}               - Get Task            ║");
            logger.info("║    POST /tasks/{taskId}:cancel        - Cancel Task         ║");
            logger.info("╚══════════════════════════════════════════════════════════════╝");
            logger.info("");
        };
    }
}
