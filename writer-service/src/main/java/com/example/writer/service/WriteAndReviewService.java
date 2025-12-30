package com.example.writer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;

import ch.qos.logback.core.util.StringUtil;
import reactor.core.publisher.Flux;

/**
 * 主 Agent 服务：封装写作和评审的完整流程
 * 主 Agent (writerAgent) 会先生成文章，然后通过 A2A 协议调用 Reviewer Agent 进行评审
 */
@Service
public class WriteAndReviewService {

    private final ReactAgent writerAgent;
    private final ObjectProvider<A2aRemoteAgent> reviewerRemoteAgentProvider;

    public WriteAndReviewService(ReactAgent writerAgent, 
                                 ObjectProvider<A2aRemoteAgent> reviewerRemoteAgentProvider) {
        this.writerAgent = writerAgent;
        this.reviewerRemoteAgentProvider = reviewerRemoteAgentProvider;
    }

    /**
     * 主 Agent 的完整工作流程：
     * 1. Writer Agent 根据主题生成文章
     * 2. 通过 A2A 协议调用 Reviewer Agent 进行评审和修改
     * 
     * @param topic 文章主题
     * @return 评审后的文章
     */
    public String writeAndReview(String topic) {
        try {
            // 步骤 1: Writer Agent 生成文章
            String prompt = "请根据以下主题写一篇文章：" + topic;
            AssistantMessage articleMessage = writerAgent.call(prompt);
            String originalArticle = articleMessage.getText();

            // 步骤 2: 通过 A2A 协议调用 Reviewer Agent
            A2aRemoteAgent reviewerRemoteAgent = reviewerRemoteAgentProvider.getIfAvailable();
            if (reviewerRemoteAgent == null || StringUtil.isNullOrEmpty(originalArticle)) {
                System.out.println("Reviewer Service 不可用或原始文章为空");
                // 如果 Reviewer Service 不可用，返回原始文章
                return originalArticle;
            }
            System.out.println("Reviewer Service 可用，开始评审:" + originalArticle);

            String reviewPrompt = "请对以下文章进行评审和修改：\n\n" + originalArticle;
            Optional<OverAllState> reviewState = reviewerRemoteAgent.invoke(reviewPrompt);
            String reviewedArticle = extractArticle(reviewState);

            // 如果评审失败，返回原始文章
            return reviewedArticle.isEmpty() ? originalArticle : reviewedArticle;
        } catch (Exception e) {
            throw new RuntimeException("文章生成或评审失败: " + e.getMessage(), e);
        }
    }


    /**
     * 仅调用主 Agent 生成文章（不进行评审）
     */
    public String writeOnly(String topic) {
        try {
            String prompt = "请根据以下主题写一篇文章：" + topic;
            AssistantMessage articleMessage = writerAgent.call(prompt);
            return articleMessage.getText();
        } catch (Exception e) {
            throw new RuntimeException("文章生成失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractArticle(Optional<OverAllState> state) {
        if (state.isEmpty()) {
            return "";
        }

        OverAllState s = state.get();

        // 尝试从 outputKey "article" 获取
        if (s.data().containsKey("article")) {
            Object articleObj = s.data().get("article");
            
            // 处理 streaming 模式返回的 Flux
            if (articleObj instanceof Flux) {
                Flux<GraphResponse<NodeOutput>> flux = (Flux<GraphResponse<NodeOutput>>) articleObj;
                StringBuilder result = new StringBuilder();
                flux.filter(response -> !response.isDone())
                    .map(response -> {
                        try {
                            NodeOutput output = response.getOutput().join();
                            if (output != null) {
                                return output.toString();
                            }
                            return "";
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .doOnNext(result::append)
                    .blockLast();
                return result.toString();
            }
            
            if (articleObj instanceof String article) {
                // 移除可能的 Agent State 前缀
                if (article.startsWith("Agent State:")) {
                    int newlineIndex = article.indexOf('\n');
                    if (newlineIndex > 0) {
                        article = article.substring(newlineIndex + 1).trim();
                    } else {
                        // 尝试移除 "Agent State: xxx" 格式的前缀
                        article = article.replaceFirst("^Agent State:\\s*\\w+\\s*", "").trim();
                    }
                }
                return article;
            }
        }

        // 尝试从 messages 获取
        if (s.data().containsKey("messages")) {
            Object messagesObj = s.data().get("messages");
            
            // 处理 streaming 模式返回的 Flux
            if (messagesObj instanceof Flux) {
                Flux<GraphResponse<NodeOutput>> flux = (Flux<GraphResponse<NodeOutput>>) messagesObj;
                StringBuilder result = new StringBuilder();
                flux.filter(response -> !response.isDone())
                    .map(response -> {
                        try {
                            NodeOutput output = response.getOutput().join();
                            if (output != null) {
                                return output.toString();
                            }
                            return "";
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .doOnNext(result::append)
                    .blockLast();
                return result.toString();
            }
            
            if (messagesObj instanceof List) {
                List<Message> messages = (List<Message>) messagesObj;
                return messages.stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(msg -> (AssistantMessage) msg)
                        .reduce((first, second) -> second)
                        .map(AssistantMessage::getText)
                        .orElse("");
            }
        }

        return "";
    }
}

