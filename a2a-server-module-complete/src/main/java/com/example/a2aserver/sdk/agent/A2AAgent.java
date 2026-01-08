package com.example.a2aserver.sdk.agent;

import java.util.Map;

/**
 * A2A Agent 接口
 *
 * 用户只需要实现这个接口，定义自己的 Agent 业务逻辑
 *
 * @param <C> 上下文类型
 */
public interface A2AAgent<C> {

    /**
     * 获取 Agent 名称
     */
    String getName();

    /**
     * 获取 Agent 描述
     */
    String getDescription();

    /**
     * 获取 Agent 版本
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取 Agent 能力列表
     */
    default AgentCapability[] getCapabilities() {
        return new AgentCapability[]{
            AgentCapability.CHAT,
            AgentCapability.STREAMING
        };
    }

    /**
     * 创建执行上下文
     */
    C createContext(Map<String, Object> params);

    /**
     * 执行 Agent 逻辑
     *
     * @param input 用户输入
     * @param context 执行上下文
     * @return Agent 响应
     */
    AgentResponse execute(String input, C context);

    /**
     * 是否支持流式响应
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Agent 能力枚举
     */
    enum AgentCapability {
        CHAT,           // 聊天
        STREAMING,      // 流式响应
        TASK_MANAGEMENT, // 任务管理
        TOOLS,          // 工具调用
        MEMORY          // 记忆管理
    }

    /**
     * Agent 响应
     */
    class AgentResponse {
        private final String content;
        private final boolean finished;
        private final Map<String, Object> metadata;

        public AgentResponse(String content) {
            this(content, true, Map.of());
        }

        public AgentResponse(String content, boolean finished, Map<String, Object> metadata) {
            this.content = content;
            this.finished = finished;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public boolean isFinished() {
            return finished;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String content;
            private boolean finished = true;
            private Map<String, Object> metadata = Map.of();

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder finished(boolean finished) {
                this.finished = finished;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public AgentResponse build() {
                return new AgentResponse(content, finished, metadata);
            }
        }
    }

    /**
     * Agent 上下文接口
     */
    interface AgentContext {
        String getTaskId();
        String getContextId();
        Map<String, Object> getAttributes();

        default void setAttribute(String key, Object value) {
            getAttributes().put(key, value);
        }

        default <T> T getAttribute(String key, Class<T> type) {
            Object value = getAttributes().get(key);
            return type.cast(value);
        }
    }
}
