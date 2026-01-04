package com.example.a2aclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A2A HTTP REST Client Demo Application
 * 基于 A2A Java SDK v0.3.3.Final 构建的 HTTP REST Client
 */
@SpringBootApplication
public class A2aClientModuleApplication {

    private static final Logger logger = LoggerFactory.getLogger(A2aClientModuleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(A2aClientModuleApplication.class, args);
    }

    @Bean
    CommandLineRunner startupInfo(@Value("${server.port:7001}") int httpPort,
                                  @Value("${a2a.server.url:http://localhost:7002}") String serverUrl) {
        return args -> {
            logger.info("");
            logger.info("╔══════════════════════════════════════════════════════════════╗");
            logger.info("║           A2A REST Client (SDK v0.3.3.Final)                ║");
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Client Port:  {}                                           ║", httpPort);
            logger.info("║  Server URL:   {}                              ║", serverUrl);
            logger.info("╠══════════════════════════════════════════════════════════════╣");
            logger.info("║  Test Endpoints:                                             ║");
            logger.info("║    GET  /test/agent-card     - Fetch Agent Card             ║");
            logger.info("║    POST /test/send?msg=xxx   - Send Message                 ║");
            logger.info("║    POST /test/stream?msg=xxx - Streaming Message            ║");
            logger.info("╚══════════════════════════════════════════════════════════════╝");
            logger.info("");
        };
    }
}
