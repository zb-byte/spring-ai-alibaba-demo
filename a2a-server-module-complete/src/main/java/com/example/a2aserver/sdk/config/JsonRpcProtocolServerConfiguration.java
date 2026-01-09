package com.example.a2aserver.sdk.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.protocol.impl.JsonRpcProtocolServer;

/**
 * JSON-RPC 协议服务器配置类
 *
 * 将 JsonRpcProtocolServer 注册为 Spring Bean，以便 REST 端点生效
 * 
 * 注意：虽然通过工厂创建，但需要注册为 Bean 以便 Spring 管理 REST 端点
 */
@Configuration
public class JsonRpcProtocolServerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "a2a.server", name = "json-rpc-enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcProtocolServer jsonRpcProtocolServer(
            A2AAgent<?> agent,
            ApplicationContext context,
            A2AServerProperties properties) {
        return new JsonRpcProtocolServer(agent, context, properties);
    }
}
