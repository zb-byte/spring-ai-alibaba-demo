package com.example.a2aclient.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.a2aclient.client.A2ARestClient;

import io.a2a.spec.AgentCard;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

@Service
public class RestDemo {

    private static final Logger logger = LoggerFactory.getLogger(RestDemo.class);
    
    private final A2ARestClient a2aClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public RestDemo(A2ARestClient a2aClient) {
        this.a2aClient = a2aClient;
    }

    public void getAgentCard() {
        try {
            AgentCard card = a2aClient.fetchAgentCard();
            logger.info("get agent card: {}", card);
        } catch (Exception e) {
            logger.error("Failed to fetch agent card", e);
        }
    }

    public void sendMessage(String msg) {
        try {
            logger.info("Test: sending message: {}", msg);
            EventKind result = a2aClient.sendMessage(msg);
            Map<String, Object> response = new HashMap<>();
            if (result instanceof Message message) {
                response.put("type", "message");
                response.put("messageId", message.getMessageId());
                response.put("role", message.getRole().asString());
                response.put("text", extractText(message));
                logger.info("Test: received message response - messageId: {}, role: {}, text: {}", 
                    message.getMessageId(), message.getRole().asString(), extractText(message));
            } else if (result instanceof Task task) {
                response.put("type", "task");
                response.put("taskId", task.getId());
                response.put("status", task.getStatus().state().asString());
                logger.info("Test: received task response - taskId: {}, status: {}", 
                    task.getId(), task.getStatus().state().asString());
                
                // 如果任务包含 artifacts，打印内容
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    task.getArtifacts().forEach(artifact -> {
                        if (artifact.parts() != null) {
                            artifact.parts().forEach(part -> {
                                if (part instanceof TextPart textPart) {
                                    logger.info("Test: artifact text: {}", textPart.getText());
                                }
                            });
                        }
                    });
                }
            }
            logger.info("Test: sent message completed");
        } catch (Exception e) {
            logger.error("Failed to send message", e);
        }
    }

    /**
     * 发送流式消息（用于 REST API，返回 SseEmitter）
     */
    public SseEmitter sendMessageStreaming(@RequestParam String msg) {
        SseEmitter emitter = new SseEmitter(60000L);
        
        executor.submit(() -> {
            try {
                logger.info("Test: sending streaming message: {}", msg);
                List<Map<String, Object>> events = new ArrayList<>();
                
                a2aClient.sendMessageStreaming(msg, event -> {
                    try {
                        Map<String, Object> eventData = new HashMap<>();
                        if (event instanceof Message message) {
                            eventData.put("type", "message");
                            eventData.put("messageId", message.getMessageId());
                            eventData.put("role", message.getRole().asString());
                            eventData.put("text", extractText(message));
                        } else if (event instanceof Task task) {
                            eventData.put("type", "task");
                            eventData.put("taskId", task.getId());
                            eventData.put("status", task.getStatus().state().asString());
                        }
                        events.add(eventData);
                        emitter.send(SseEmitter.event().data(eventData));
                    } catch (Exception e) {
                        logger.error("Error sending SSE event", e);
                    }
                });
                
                emitter.complete();
            } catch (Exception e) {
                logger.error("Streaming error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    /**
     * 发送流式消息（用于命令行测试）
     */
    public void sendMessageStreamingForConsole(String msg) {
        try {
            logger.info("Test: sending streaming message: {}", msg);
            StringBuilder fullText = new StringBuilder();
            String[] taskId = new String[1];
            
            a2aClient.sendMessageStreaming(msg, event -> {
                if (event instanceof Message message) {
                    String text = extractText(message);
                    if (!text.isEmpty()) {
                        fullText.append(text);
                        // 实时打印流式内容
                        System.out.print(text);
                        System.out.flush();
                    }
                    logger.debug("Test: received streaming message - messageId: {}, role: {}, text: {}", 
                        message.getMessageId(), message.getRole().asString(), text);
                } else if (event instanceof Task task) {
                    taskId[0] = task.getId();
                    logger.info("Test: received streaming task - taskId: {}, status: {}", 
                        task.getId(), task.getStatus().state().asString());
                    
                    if (task.getStatus().state() == io.a2a.spec.TaskState.COMPLETED) {
                        System.out.println(); // 换行
                        logger.info("Test: streaming completed - full text length: {}", fullText.length());
                    }
                }
            });
            
            logger.info("Test: streaming message completed");
        } catch (Exception e) {
            logger.error("Failed to send streaming message", e);
        }
    }

    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        try {
            Task task = a2aClient.getTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("contextId", task.getContextId());
            result.put("status", task.getStatus().state().asString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get task", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取任务（用于命令行测试）
     */
    public void getTaskForConsole(String taskId) {
        try {
            logger.info("Test: getting task: {}", taskId);
            Task task = a2aClient.getTask(taskId);
            logger.info("Test: task info - taskId: {}, contextId: {}, status: {}", 
                task.getId(), task.getContextId(), task.getStatus().state().asString());
            
            if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                task.getArtifacts().forEach(artifact -> {
                    logger.info("Test: artifact - name: {}, artifactId: {}", 
                        artifact.name(), artifact.artifactId());
                    if (artifact.parts() != null) {
                        artifact.parts().forEach(part -> {
                            if (part instanceof TextPart textPart) {
                                logger.info("Test: artifact text: {}", textPart.getText());
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Failed to get task", e);
        }
    }

    private String extractText(Message message) {
        if (message.getParts() == null || message.getParts().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : message.getParts()) {
            if (part instanceof TextPart textPart) {
                sb.append(textPart.getText());
            }
        }
        return sb.toString();
    }
}
