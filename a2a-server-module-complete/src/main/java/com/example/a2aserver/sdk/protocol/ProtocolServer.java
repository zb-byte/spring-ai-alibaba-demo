package com.example.a2aserver.sdk.protocol;

import io.a2a.spec.AgentCard;

/**
 * 协议服务器接口
 *
 * 每种协议都需要实现这个接口
 */
public interface ProtocolServer {

    /**
     * 获取协议类型
     */
    ProtocolType getProtocolType();

    /**
     * 启动服务器
     */
    void start(AgentCard agentCard) throws Exception;

    /**
     * 停止服务器
     */
    void stop() throws Exception;

    /**
     * 获取服务器端口
     */
    int getPort();

    /**
     * 检查服务器是否正在运行
     */
    boolean isRunning();

    /**
     * 获取服务器地址
     */
    default String getServerUrl() {
        return String.format("http://localhost:%d", getPort());
    }
}
