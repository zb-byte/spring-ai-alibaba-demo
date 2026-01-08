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
 */
public class DefaultProtocolServerFactory implements ProtocolServerFactory {

    private final A2AServerProperties properties;
    private final ApplicationContext applicationContext;

    public DefaultProtocolServerFactory() {
        this(new A2AServerProperties(), null);
    }

    public DefaultProtocolServerFactory(A2AServerProperties properties,
                                       ApplicationContext applicationContext) {
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
        return true; // 支持所有协议类型
    }
}
