package com.example.a2aserver.sdk.protocol.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolServer;
import com.example.a2aserver.sdk.protocol.ProtocolType;

import io.a2a.spec.AgentCard;

/**
 * 协议服务器抽象基类
 *
 * 提供通用的服务器实现逻辑
 */
public abstract class AbstractProtocolServer implements ProtocolServer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final A2AAgent<?> agent;
    protected final ApplicationContext applicationContext;
    protected final A2AServerProperties properties;
    protected volatile boolean running = false;
    protected int port;

    public AbstractProtocolServer(A2AAgent<?> agent,
                                  ApplicationContext applicationContext,
                                  A2AServerProperties properties) {
        this.agent = agent;
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    @Override
    public void start(AgentCard agentCard) throws Exception {
        if (running) {
            logger.warn("{} server is already running", getProtocolType().getCode());
            return;
        }

        logger.info("Starting {} server on port {}...", getProtocolType().getCode(), port);

        // 构建 AgentCard
        if (agentCard == null) {
            agentCard = buildAgentCard();
        }

        // 启动服务器
        doStart(agentCard);

        running = true;
        logger.info("{} server started successfully", getProtocolType().getCode());
    }

    @Override
    public void stop() throws Exception {
        if (!running) {
            logger.warn("{} server is not running", getProtocolType().getCode());
            return;
        }

        logger.info("Stopping {} server...", getProtocolType().getCode());
        doStop();
        running = false;
        logger.info("{} server stopped successfully", getProtocolType().getCode());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPort() {
        return port;
    }

    /**
     * 构建AgentCard
     */
    protected abstract AgentCard buildAgentCard();

    /**
     * 启动服务器（子类实现）
     */
    protected abstract void doStart(AgentCard agentCard) throws Exception;

    /**
     * 停止服务器（子类实现）
     */
    protected abstract void doStop() throws Exception;

    /**
     * 获取服务器URL
     */
    public String getServerUrl() {
        return String.format("http://%s:%d", properties.getHost(), port);
    }
}
