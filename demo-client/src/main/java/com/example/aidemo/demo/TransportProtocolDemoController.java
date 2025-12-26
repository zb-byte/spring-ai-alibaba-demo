package com.example.aidemo.demo;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.a2a.core.constants.A2aConstants;
import io.a2a.spec.AgentCard;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 三种传输协议的演示控制器
 * 
 * 本控制器演示了如何使用 Spring AI Alibaba 支持的三种 A2A 传输协议：
 * 1. JSON-RPC 2.0
 * 2. gRPC
 * 3. HTTP+JSON/REST
 * 
 * 通过配置 spring.ai.alibaba.a2a.server.type 来切换不同的协议类型
 * 
 * @author spring-ai-alibaba-demo
 */
@RestController
@RequestMapping("/demo/transport")
public class TransportProtocolDemoController {

    private final ReactAgent localAgent;
    private final A2aRemoteAgent remoteAgent;
    private final AgentCard agentCard;

    @Value("${spring.ai.alibaba.a2a.server.type:JSONRPC}")
    private String currentTransportType;

    public TransportProtocolDemoController(
            ReactAgent localAgent,
            @Qualifier("remoteReactAgent") A2aRemoteAgent remoteAgent,
            AgentCard agentCard) {
        this.localAgent = localAgent;
        this.remoteAgent = remoteAgent;
        this.agentCard = agentCard;
    }

    /**
     * 获取当前使用的传输协议类型
     */
    @GetMapping("/type")
    public ResponseEntity<Map<String, Object>> getCurrentTransportType() {
        return ResponseEntity.ok(Map.of(
                "currentTransportType", currentTransportType,
                "supportedTypes", List.of(
                        A2aConstants.AGENT_TRANSPORT_TYPE_JSON_RPC,
                        A2aConstants.AGENT_TRANSPORT_TYPE_GRPC,
                        A2aConstants.AGENT_TRANSPORT_TYPE_REST
                ),
                "agentCard", Map.of(
                        "name", agentCard.name(),
                        "description", agentCard.description(),
                        "url", agentCard.url(),
                        "capabilities", Map.of(
                                "streaming", agentCard.capabilities().streaming()
                        )
                )
        ));
    }

    /**
     * 使用本地 Agent 进行对话（不通过 A2A 协议）
     */
    @PostMapping("/local")
    public ResponseEntity<Map<String, Object>> chatLocal(@RequestBody Map<String, String> request) {
        String input = request.get("message");
        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }

        try {
            AssistantMessage message = localAgent.call(input);
            return ResponseEntity.ok(Map.of(
                    "transport", "local",
                    "input", input,
                    "response", message != null ? message.getText() : "",
                    "protocol", "N/A (本地调用)"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 通过 A2A 协议（当前配置的传输协议）进行对话
     */
    @PostMapping("/a2a")
    public ResponseEntity<Map<String, Object>> chatViaA2a(@RequestBody Map<String, String> request) {
        String input = request.get("message");
        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }

        try {
            Optional<OverAllState> state = remoteAgent.invoke(input);
            String response = extractAssistantReply(state);

            return ResponseEntity.ok(Map.of(
                    "transport", "a2a",
                    "protocol", currentTransportType,
                    "input", input,
                    "response", response,
                    "agentCard", Map.of(
                            "name", agentCard.name(),
                            "url", agentCard.url()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "protocol", currentTransportType
                    ));
        }
    }

    /**
     * 获取 Agent Card 信息（通过 /.well-known/agent.json 端点）
     */
    @GetMapping("/agent-card")
    public ResponseEntity<AgentCard> getAgentCard() {
        return ResponseEntity.ok(agentCard);
    }

    /**
     * 调试端点：查看 A2A 调用返回的原始数据结构
     */
    @PostMapping("/a2a/debug")
    public ResponseEntity<Map<String, Object>> debugA2a(@RequestBody Map<String, String> request) {
        String input = request.get("message");
        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }

        try {
            Optional<OverAllState> state = remoteAgent.invoke(input);
            
            Map<String, Object> debugInfo = new java.util.HashMap<>();
            debugInfo.put("statePresent", state.isPresent());
            
            if (state.isPresent()) {
                OverAllState s = state.get();
                debugInfo.put("dataKeys", s.data().keySet());
                debugInfo.put("dataSize", s.data().size());
                
                // 显示所有 key 的值类型
                Map<String, String> keyTypes = new java.util.HashMap<>();
                for (String key : s.data().keySet()) {
                    Object value = s.data().get(key);
                    keyTypes.put(key, value != null ? value.getClass().getName() : "null");
                }
                debugInfo.put("keyTypes", keyTypes);
                
                // 尝试提取 messages
                if (s.data().containsKey("messages")) {
                    Object messagesObj = s.data().get("messages");
                    debugInfo.put("messagesType", messagesObj != null ? messagesObj.getClass().getName() : "null");
                    if (messagesObj instanceof List) {
                        debugInfo.put("messagesSize", ((List<?>) messagesObj).size());
                    }
                }
                
                // 显示完整的数据（限制大小）
                Map<String, Object> dataPreview = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : s.data().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        dataPreview.put(entry.getKey(), value);
                    } else if (value instanceof List) {
                        dataPreview.put(entry.getKey(), "List[" + ((List<?>) value).size() + "]");
                    } else {
                        dataPreview.put(entry.getKey(), value != null ? value.toString() : "null");
                    }
                }
                debugInfo.put("dataPreview", dataPreview);
            }
            
            String response = extractAssistantReply(state);
            debugInfo.put("extractedResponse", response);
            debugInfo.put("responseLength", response != null ? response.length() : 0);
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "errorType", e.getClass().getName(),
                            "stackTrace", java.util.Arrays.toString(e.getStackTrace())
                    ));
        }
    }

    /**
     * 从 OverAllState 中提取 Assistant 的回复
     */
    @SuppressWarnings("unchecked")
    private String extractAssistantReply(Optional<OverAllState> state) {
        if (state.isEmpty()) {
            return "";
        }
        
        OverAllState s = state.get();
        
        // 首先尝试从 outputKey 获取（A2aRemoteAgent 可能使用不同的 outputKey）
        // 检查所有可能的 key
        if (s.data().containsKey("messages")) {
            Object messagesObj = s.data().get("messages");
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
        
        // 尝试从其他可能的 key 获取
        for (String key : s.data().keySet()) {
            Object value = s.data().get(key);
            if (value instanceof AssistantMessage) {
                return ((AssistantMessage) value).getText();
            }
            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }
        
        return "";
    }
}

