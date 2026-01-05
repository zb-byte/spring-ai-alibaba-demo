package com.example.writer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;

import reactor.core.publisher.Flux;

/**
 * 主 Agent 服务：封装写作和评审的完整流程
 * 主 Agent (writerAgent) 会先生成文章，然后通过 A2A 协议调用 Reviewer Agent 进行评审
 */
@Service
public class A2ADemoService {

    private final A2aRemoteAgent a2aRemoteAgent;

    public A2ADemoService(A2aRemoteAgent a2aRemoteAgent) {
        this.a2aRemoteAgent = a2aRemoteAgent;
    }

    /**
     * A2A 协议，spring-ai-alibaba 默认是 JSONRPC 的方式调用 远程智能体
     */
    public void a2aDemo() {
        try {
            String reviewPrompt = "请对以下文章进行评审：人工智能是一场新的工业革命\n\n";
            System.out.println("=====a2a调用开始=====："+reviewPrompt);
            Optional<OverAllState> reviewState = a2aRemoteAgent.invoke(reviewPrompt);
            System.out.println("=====a2a调用完成=====："+reviewState);
            System.out.println("=====a2a响应解析开始=====");
            String re = extractArticle(reviewState);
            System.out.println("=====a2a响应解析完成=====：\n\n"+re);
        } catch (Exception e) {
            throw new RuntimeException("文章生成或评审失败: " + e.getMessage(), e);
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

