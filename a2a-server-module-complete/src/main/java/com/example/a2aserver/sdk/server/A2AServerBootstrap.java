package com.example.a2aserver.sdk.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.example.a2aserver.sdk.protocol.ProtocolServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolServer;
import com.example.a2aserver.sdk.protocol.ProtocolType;

/**
 * A2A 服务器启动器（建造者模式）
 *
 * 统一的入口，负责启动和管理多种协议的服务器
 */
public class A2AServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(A2AServerBootstrap.class);

    private final A2AAgent<?> agent;
    private final ApplicationContext applicationContext;
    private final Set<ProtocolType> enabledProtocols;
    private final A2AServerProperties properties;

    private final List<ProtocolServer> servers = new ArrayList<>();

    private A2AServerBootstrap(Builder builder) {
        this.agent = builder.agent;
        this.applicationContext = builder.applicationContext;
        this.enabledProtocols = builder.enabledProtocols;
        this.properties = builder.properties;
    }

    /**
     * 启动所有启用的协议服务器
     */
    public A2AServerBootstrap start() {
        logger.info("===========================================");
        logger.info("Starting A2A Server: {}", agent.getName());
        logger.info("Description: {}", agent.getDescription());
        logger.info("===========================================");

        ProtocolServerFactory factory = new DefaultProtocolServerFactory();

        for (ProtocolType protocol : enabledProtocols) {
            try {
                if (properties.isProtocolEnabled(protocol)) {
                    ProtocolServer server = factory.createServer(protocol, agent, applicationContext);
                    server.start(null); // AgentCard 会在 start 内部构建
                    servers.add(server);

                    logger.info("✓ {} server started on port {}",
                            protocol.getCode(),
                            server.getPort());
                } else {
                    logger.info("- {} server is disabled", protocol.getCode());
                }
            } catch (Exception e) {
                logger.error("Failed to start {} server: {}", protocol.getCode(), e.getMessage(), e);
            }
        }

        logger.info("===========================================");
        logger.info("All servers started successfully!");
        logger.info("===========================================");

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        return this;
    }

    /**
     * 停止所有服务器
     */
    public void stop() {
        logger.info("Shutting down A2A Server...");

        for (ProtocolServer server : servers) {
            try {
                if (server.isRunning()) {
                    server.stop();
                    logger.info("✓ {} server stopped", server.getProtocolType().getCode());
                }
            } catch (Exception e) {
                logger.error("Failed to stop {} server: {}",
                        server.getProtocolType().getCode(), e.getMessage());
            }
        }

        logger.info("A2A Server stopped");
    }

    /**
     * 获取所有已启动的服务器
     */
    public List<ProtocolServer> getServers() {
        return List.copyOf(servers);
    }

    /**
     * 建造者
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private A2AAgent<?> agent;
        private ApplicationContext applicationContext;
        private Set<ProtocolType> enabledProtocols = EnumSet.allOf(ProtocolType.class);
        private A2AServerProperties properties = new A2AServerProperties();

        public Builder agent(A2AAgent<?> agent) {
            this.agent = agent;
            return this;
        }

        public Builder applicationContext(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            return this;
        }

        public Builder enableProtocols(ProtocolType... protocols) {
            this.enabledProtocols = EnumSet.noneOf(ProtocolType.class);
            this.enabledProtocols.addAll(List.of(protocols));
            return this;
        }

        public Builder enableAllProtocols() {
            this.enabledProtocols = EnumSet.allOf(ProtocolType.class);
            return this;
        }

        public Builder properties(A2AServerProperties properties) {
            this.properties = properties;
            return this;
        }

        public A2AServerBootstrap build() {
            if (agent == null) {
                throw new IllegalArgumentException("Agent cannot be null");
            }
            if (applicationContext == null) {
                throw new IllegalArgumentException("ApplicationContext cannot be null");
            }
            return new A2AServerBootstrap(this);
        }
    }
}
