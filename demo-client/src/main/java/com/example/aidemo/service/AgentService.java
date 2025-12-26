package com.example.aidemo.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentService {

    private final ReactAgent localAgent;

    private final A2aRemoteAgent remoteAgent;

    public AgentService(ReactAgent reactAgent,
                        @Qualifier("remoteReactAgent") A2aRemoteAgent remoteAgent) {
        this.localAgent = reactAgent;
        this.remoteAgent = remoteAgent;
    }

    public String chatLocal(String input) throws Exception {
        AssistantMessage message = localAgent.call(input);
        return message != null ? message.getText() : "";
    }

    public String chatViaA2a(String input) throws Exception {
        Optional<OverAllState> state = remoteAgent.invoke(input);
        return extractAssistantReply(state);
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantReply(Optional<OverAllState> state) {
        return state.flatMap(s -> s.value("messages"))
                .map(messages -> (List<Message>) messages)
                .stream()
                .flatMap(List::stream)
                .filter(msg -> msg instanceof AssistantMessage)
                .map(msg -> (AssistantMessage) msg)
                .reduce((first, second) -> second)
                .map(AssistantMessage::getText)
                .orElse("");
    }
}

