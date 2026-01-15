package com.example.a2aserver.example;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import com.example.a2aserver.sdk.agent.A2AAgent;

/**
 * 示例 Agent 实现
 * 废弃
 * 用户只需要实现 A2AAgent 接口，定义自己的业务逻辑
 * SDK 会自动处理所有协议相关的细节
 *
 */
@Deprecated
@Component
public class MyAgent implements A2AAgent<MyAgent.MyContext> {

    private static final Logger logger = LoggerFactory.getLogger(MyAgent.class);

    private final ChatClient chatClient;

    public MyAgent(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一个友好的 AI 助手，名字叫 Demo Agent。
                    你会根据用户的喜好推荐中国的八大菜系中的菜品。
                    你会用简洁、有帮助的方式回答用户的问题。
                    """)
                .build();
    }

    @Override
    public String getName() {
        return "推荐菜品agent";
    }

    @Override
    public String getDescription() {
        return "一个根据用户的喜好推荐中国的八大菜系中的菜品的agent";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public MyContext createContext(Map<String, Object> params) {
        return new MyContext();
    }

    @Override
    public AgentResponse execute(String input, MyContext context) {
        logger.info("MyAgent Executing agent with input: {}", input);

        try {
            // 调用 LLM
            String response = chatClient.prompt()
                    .user(input)
                    .call()
                    .content();

            logger.info("MyAgent Agent response: {}", response);

            // 返回响应
            return AgentResponse.builder()
                    .content(response)
                    .finished(true)
                    .metadata(Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "model", "demo-model"
                    ))
                    .build();

        } catch (Exception e) {
            logger.error("s Error executing agent", e);

            // 返回错误响应
            return AgentResponse.builder()
                    .content("抱歉，处理您的请求时出错: " + e.getMessage())
                    .finished(true)
                    .build();
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * 自定义上下文
     *
     * 用户可以在上下文中存储任意数据
     */
    public static class MyContext implements AgentContext {
        private final Map<String, Object> attributes = new java.util.HashMap<>();

        @Override
        public String getTaskId() {
            return (String) attributes.getOrDefault("taskId", "default-task");
        }

        @Override
        public String getContextId() {
            return (String) attributes.getOrDefault("contextId", "default-context");
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        // 添加自定义方法
        public void setCustomData(String key, Object value) {
            attributes.put(key, value);
        }

        public <T> T getCustomData(String key, Class<T> type) {
            return type.cast(attributes.get(key));
        }
    }
}
