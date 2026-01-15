package com.example.writer.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.io.grpc.internal.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

@Configuration
public class AgentConfiguration {

    @Value("${reviewer.agent.url:http://localhost:7003/a2a}")
    private String reviewerAgentUrl;

    /**
     * 根据well-known URL 获取 Agent Card
     * @return
     */
    @Bean
    public AgentCardProvider reviewerAgentCardProvider() {
        String wellKnownUrl = "http://localhost:7003/a2a";
        return RemoteAgentCardProvider.newProvider(wellKnownUrl);
    }

    /**
     * 根据 Agent Card 获取 A2aRemoteAgent
     * @param reviewerAgentCardProvider
     * @return
     */
    @Bean
    public A2aRemoteAgent reviewerRemoteAgent(AgentCardProvider reviewerAgentCardProvider)  {
        return A2aRemoteAgent.builder()
                .name("reviewer-remote-agent")
                .description("通过 A2A 协议调用 Reviewer Service，可以对文章进行评审和修改")
                .instruction("{input}")
                .agentCardProvider(reviewerAgentCardProvider)
                .outputKey("article")
                .build();
    }
}

