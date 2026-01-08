package com.example.a2aserver.sdk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.server.A2AServerBootstrap;

/**
 * A2A 服务器自动配置类
 *
 * Spring Boot 自动配置，扫描并启动 A2A 服务器
 */
@AutoConfiguration
@EnableConfigurationProperties({A2AServerProperties.class, A2AServerPropertiesConfiguration.class})
@ComponentScan(basePackages = "com.example.a2aserver.sdk")
public class A2AServerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(A2AServerAutoConfiguration.class);

    @Autowired(required = false)
    private A2AAgent<?> agent;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private A2AServerProperties properties;

    /**
     * 自动启动 A2A 服务器
     *
     * 如果用户实现了 A2AAgent 接口，则自动启动所有启用的协议服务器
     */
    @org.springframework.context.annotation.Bean
    @ConditionalOnProperty(prefix = "a2a.server", name = "auto-start", havingValue = "true", matchIfMissing = true)
    public A2AServerBootstrap a2aServerBootstrap() {
        if (agent == null) {
            logger.warn("No A2AAgent implementation found. Skipping auto-start.");
            logger.info("To use A2A Server, please implement the A2AAgent interface.");
            return null;
        }

        logger.info("Found A2AAgent implementation: {}", agent.getName());
        logger.info("Auto-starting A2A Server with protocols: {}", getEnabledProtocols());

        // 构建并启动服务器
        A2AServerBootstrap bootstrap = A2AServerBootstrap.builder()
                .agent(agent)
                .applicationContext(applicationContext)
                .properties(properties)
                .build();

        // 在 Spring Boot 启动后启动服务器
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (bootstrap != null) {
                bootstrap.stop();
            }
        }));

        // 异步启动，避免阻塞 Spring Boot 启动
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待 Spring Boot 完全启动
                bootstrap.start();
            } catch (Exception e) {
                logger.error("Failed to start A2A Server", e);
            }
        }).start();

        return bootstrap;
    }

    private String getEnabledProtocols() {
        StringBuilder sb = new StringBuilder();
        if (properties.isRestEnabled()) {
            sb.append("REST ");
        }
        if (properties.isGrpcEnabled()) {
            sb.append("gRPC ");
        }
        if (properties.isJsonRpcEnabled()) {
            sb.append("JSON-RPC ");
        }
        return sb.toString();
    }
}
