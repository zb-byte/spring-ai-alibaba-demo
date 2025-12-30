package com.example.writer.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.example.writer.service.AgentRegistry;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Value("${reviewer.agent.url:http://127.0.0.1:8081}")
    private String reviewerAgentUrl;

    @Bean
    public OpenAiChatModel openAiChatModel(LlmProperties properties) {
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(properties.apiKey());

        if (StringUtils.hasText(properties.baseUrl())) {
            apiBuilder.baseUrl(properties.baseUrl());
        }

        if (StringUtils.hasText(properties.completionsPath())) {
            apiBuilder.completionsPath(properties.completionsPath());
        }

        if (StringUtils.hasText(properties.embeddingsPath())) {
            apiBuilder.embeddingsPath(properties.embeddingsPath());
        }

        return OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.modelName())
                        .build())
                .openAiApi(apiBuilder.build())
                .build();
    }

    @Bean
    public ReactAgent writerAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder()
                .name("writer-agent")
                .description("一个专业的文章写作 Agent，可以根据主题生成文章")
                .instruction("""
                        你是一名专业的文章写手，擅长根据主题创作高质量的文章。
                        请根据用户提供的主题，创作一篇结构清晰、内容丰富的文章。
                        文章应该：
                        1. 有明确的主题和观点
                        2. 逻辑清晰，层次分明
                        3. 语言流畅，表达准确
                        4. 字数控制在 200-300 字左右
                        
                        直接返回文章内容，不要包含其他说明。
                        """)
                .model(chatModel)
                .saver(new MemorySaver())
                .outputKey("article")
                .build();
    }

    @Bean
    public AgentCardProvider reviewerAgentCardProvider() {
        // 从 Reviewer Service 的 well-known URL 获取 Agent Card
        String wellKnownUrl = reviewerAgentUrl + "/.well-known/agent.json";
        return RemoteAgentCardProvider.newProvider(wellKnownUrl);
    }

    @Bean
    public A2aRemoteAgent reviewerRemoteAgent(AgentCardProvider reviewerAgentCardProvider) throws GraphStateException {
        return A2aRemoteAgent.builder()
                .name("reviewer-remote-agent")
                .description("通过 A2A 协议调用 Reviewer Service，可以对文章进行评审和修改")
                .instruction("{input}")
                .agentCardProvider(reviewerAgentCardProvider)
                .outputKey("article")
                .build();
    }

    /**
     * Planner Agent：调度大脑
     * 功能：
     * 1. 发现 Agent - 通过 AgentRegistry 发现所有可用的 Agent
     * 2. 理解 Agent 能力 - 理解每个 Agent 能做什么
     * 3. 选择 & 调用 - 根据用户需求选择合适的 Agent 并调用
     */
    @Bean
    @Primary
    public ReactAgent plannerAgent(ChatModel chatModel, AgentRegistry agentRegistry) throws GraphStateException {
        //获取所有Agent的能力描述·
        String agentCapabilities = agentRegistry.getAllAgentCapabilities();
        
        return ReactAgent.builder()
                .name("planner-agent")
                .description("调度大脑：发现、理解并调用其他 Agent 完成任务")
                .instruction(String.format("""
                        你是一个智能调度 Agent（Planner Agent），你的职责是：
                        
                        1. **发现 Agent**：了解系统中所有可用的 Agent
                        2. **理解 Agent 能力**：理解每个 Agent 能做什么
                        3. **选择 & 调用**：根据用户需求，选择合适的 Agent 并调用它们完成任务
                        
                        ## 当前可用的 Agent：
                        %s
                        
                        ## 工作流程：
                        1. 当用户提出需求时，首先分析需求
                        2. 根据需求选择合适的 Agent（可以是一个或多个）
                        3. 按照正确的顺序调用 Agent 完成任务
                        4. 整合结果并返回给用户
                        
                        ## 调用规则：
                        - 如果需要写文章，使用 writer-agent
                        - 如果需要评审文章，使用 reviewer-remote-agent（通过 A2A 协议）
                        - 如果需要完整的写作+评审流程，先调用 writer-agent，再调用 reviewer-remote-agent
                        
                        请根据用户需求，智能地选择和调用合适的 Agent 完成任务。
                        """, agentCapabilities))
                .model(chatModel)
                .saver(new MemorySaver())
                .build();
    }
}

