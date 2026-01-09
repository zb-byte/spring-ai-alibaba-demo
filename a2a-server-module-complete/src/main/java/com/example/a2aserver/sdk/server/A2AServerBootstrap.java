package com.example.a2aserver.sdk.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.example.a2aserver.sdk.protocol.ProtocolServerFactory;
import com.example.a2aserver.sdk.protocol.ServerCreationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolServer;
import com.example.a2aserver.sdk.protocol.ProtocolType;
import com.example.a2aserver.sdk.protocol.impl.JsonRpcProtocolServer;
import com.example.a2aserver.sdk.protocol.impl.RestProtocolServer;

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
    private final ProtocolServerFactory factory;

    private final List<ProtocolServer> servers = new ArrayList<>();

    private A2AServerBootstrap(Builder builder) {
        this.agent = builder.agent;
        this.applicationContext = builder.applicationContext;
        this.enabledProtocols = builder.enabledProtocols;
        this.properties = builder.properties;
        this.factory = builder.factory;
    }

    /**
     * 启动所有启用的协议服务器
     */
    public A2AServerBootstrap start() {
        logger.info("===========================================");
        logger.info("Starting A2A Server: {}", agent.getName());
        logger.info("Description: {}", agent.getDescription());
        logger.info("===========================================");

        // 使用注入的工厂，如果没有则创建默认工厂（并传递 properties）
        ProtocolServerFactory factory = this.factory != null 
            ? this.factory 
            : new DefaultProtocolServerFactory(properties, applicationContext);

        for (ProtocolType protocol : enabledProtocols) {
            try {
                if (properties.isProtocolEnabled(protocol)) {
                    ProtocolServer server = createOrGetServer(protocol, factory);
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
     * 根据协议类型选择创建策略
     * 
     * @param protocolType 协议类型
     * @param factory 协议服务器工厂
     * @return 协议服务器实例
     */
    private ProtocolServer createOrGetServer(ProtocolType protocolType, ProtocolServerFactory factory) {
        if (protocolType.getCreationStrategy() == ServerCreationStrategy.SPRING_BEAN) {
            // 从 Spring 容器获取 Bean
            try {
                return switch (protocolType) {
                    case HTTP_REST -> applicationContext.getBean(RestProtocolServer.class);
                    default -> factory.createServer(protocolType, agent, applicationContext);
                };
            } catch (Exception e) {
                logger.warn("Failed to get {} server from Spring container, falling back to factory creation", 
                        protocolType.getCode(), e);
                // 如果获取失败，回退到工厂创建
                return factory.createServer(protocolType, agent, applicationContext);
            }
        } else {
            // 通过工厂创建，但先尝试从 Spring 容器获取（如果已注册为 Bean）
            // 这适用于 JSON-RPC 等需要 REST 端点的服务器
            try {
                return switch (protocolType) {
                    case JSON_RPC -> {
                        try {
                            yield applicationContext.getBean(JsonRpcProtocolServer.class);
                        } catch (Exception ex) {
                            logger.debug("JsonRpcProtocolServer not found in Spring container, creating via factory");
                            yield factory.createServer(protocolType, agent, applicationContext);
                        }
                    }
                    default -> factory.createServer(protocolType, agent, applicationContext);
                };
            } catch (Exception e) {
                // 如果获取失败，通过工厂创建
                return factory.createServer(protocolType, agent, applicationContext);
            }
        }
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
        private ProtocolServerFactory factory;

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

        /**
         * 设置协议服务器工厂
         * 
         * @param factory 协议服务器工厂，如果为 null，将使用默认工厂
         * @return Builder 实例
         */
        public Builder factory(ProtocolServerFactory factory) {
            this.factory = factory;
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
