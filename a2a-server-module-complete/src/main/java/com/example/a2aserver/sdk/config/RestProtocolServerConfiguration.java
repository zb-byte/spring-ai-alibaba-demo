package com.example.a2aserver.sdk.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.protocol.impl.RestProtocolServer;

/**
 * REST 协议服务器配置类
 *
 * 将 RestProtocolServer 注册为 Spring Bean，以便 Spring 注解生效
 */
@Configuration
public class RestProtocolServerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "a2a.server", name = "rest-enabled", havingValue = "true", matchIfMissing = true)
    public RestProtocolServer restProtocolServer(
            A2AAgent<?> agent,
            ApplicationContext context,
            A2AServerProperties properties) {
        return new RestProtocolServer(agent, context, properties);
    }
}
