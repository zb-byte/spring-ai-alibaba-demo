package com.example.a2aserver.sdk.server;

import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.config.A2AServerProperties;
import com.example.a2aserver.sdk.protocol.ProtocolServer;
import com.example.a2aserver.sdk.protocol.ProtocolServerFactory;
import com.example.a2aserver.sdk.protocol.ProtocolType;
import com.example.a2aserver.sdk.protocol.impl.GrpcProtocolServer;
import com.example.a2aserver.sdk.protocol.impl.JsonRpcProtocolServer;
import com.example.a2aserver.sdk.protocol.impl.RestProtocolServer;

/**
 * 默认的协议服务器工厂
 *
 * 简化实现，直接创建协议服务器实例
 * 
 * 注意：REST 服务器应该通过 Spring Bean 管理，此工厂仅用于创建 gRPC 和 JSON-RPC 服务器
 */
public class DefaultProtocolServerFactory implements ProtocolServerFactory {

    private final A2AServerProperties properties;
    private final ApplicationContext applicationContext;

    /**
     * 构造函数
     * 
     * @param properties 服务器配置属性，不能为 null
     * @param applicationContext Spring 应用上下文，不能为 null
     * @throws IllegalArgumentException 如果 properties 或 applicationContext 为 null
     */
    public DefaultProtocolServerFactory(A2AServerProperties properties,
                                       ApplicationContext applicationContext) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        if (applicationContext == null) {
            throw new IllegalArgumentException("ApplicationContext cannot be null");
        }
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public ProtocolServer createServer(ProtocolType protocolType,
                                      A2AAgent<?> agent,
                                      ApplicationContext applicationContext) {
        return switch (protocolType) {
            case HTTP_REST -> new RestProtocolServer(agent, applicationContext, properties);
            case GRPC -> new GrpcProtocolServer(agent, applicationContext, properties);
            case JSON_RPC -> new JsonRpcProtocolServer(agent, applicationContext, properties);
        };
    }

    @Override
    public boolean supports(ProtocolType protocolType) {
        return switch (protocolType) {
            case HTTP_REST, GRPC, JSON_RPC -> true;
        };
    }
}
