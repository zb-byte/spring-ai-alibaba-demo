package com.example.a2aserver.sdk.protocol;

import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;

/**
 * 协议服务器工厂（工厂模式）
 *
 * 根据协议类型创建对应的服务器实例
 */
public interface ProtocolServerFactory {

    /**
     * 创建协议服务器
     *
     * @param protocolType 协议类型
     * @param agent Agent 实例
     * @param applicationContext Spring 应用上下文
     * @return 协议服务器实例
     */
    ProtocolServer createServer(ProtocolType protocolType,
                               A2AAgent<?> agent,
                               ApplicationContext applicationContext);

    /**
     * 检查是否支持该协议类型
     */
    boolean supports(ProtocolType protocolType);
}
