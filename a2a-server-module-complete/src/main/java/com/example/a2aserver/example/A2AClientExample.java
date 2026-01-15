package com.example.a2aserver.example;

import com.example.a2aserver.sdk.model.JSONRPCResponse;
import com.example.a2aserver.sdk.model.TaskSendParams;
import io.a2a.spec.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example usage of A2A client - AI Translation Bot
 */
public class A2AClientExample {
    
    public static void main(String[] args) {
        // Create client
        A2AClient client = new A2AClient("http://localhost:7003");
        
        try {
            // Example 1: Get agent card
            System.out.println("=== Getting Translation Bot Agent Card ===");
            AgentCard agentCard = client.getAgentCard();
            System.out.println("Agent: " + agentCard.name());
            System.out.println("Description: " + agentCard.description());
            System.out.println("Version: " + agentCard.version());
            System.out.println("Skills: " + agentCard.skills());
            System.out.println();
            
            // Example 2: Translate French to Chinese
            System.out.println("=== Translating French to Chinese ===");
            
            // Create text part for French to Chinese translation
            TextPart frenchToChinesePart = new TextPart("Bonjour le monde! Comment allez-vous?", null);
            

            Message frenchToChineseMessage = new Message(
                    Message.Role.USER,
                    List.of(frenchToChinesePart),     // parts
                    UUID.randomUUID().toString(),  // messageId
                    null,                         // contextId
                    null,                         // taskId
                    null,                         // referenceTaskIds
                    null,                          // metadata
                    null,
                    Message.MESSAGE
            );
            
            TaskSendParams frenchToChineseParams = new TaskSendParams(
                "french-to-chinese-task",
                null,  // sessionId
                frenchToChineseMessage,
                null,  // pushNotification
                null,  // historyLength
                Map.of()  // metadata
            );
            
            JSONRPCResponse frenchToChineseResponse = client.sendTask(frenchToChineseParams);
            Task frenchToChineseTask = (Task) frenchToChineseResponse.result();
            System.out.println("Original French: " + frenchToChinesePart.getText());
            System.out.println("Task ID: " + frenchToChineseTask.getId());
            System.out.println("Translation Status: " + frenchToChineseTask.getStatus().state());
            
            // Print translation result if available in history
            if (frenchToChineseTask.getHistory() != null && frenchToChineseTask.getHistory().size() > 1) {
                Message lastMessage = frenchToChineseTask.getHistory().get(frenchToChineseTask.getHistory().size() - 1);
                if (lastMessage.getRole().equals("assistant") && !lastMessage.getParts().isEmpty()) {
                    Part translationPart = lastMessage.getParts().get(0);
                    if (translationPart instanceof TextPart textPart) {
                        System.out.println("Chinese Translation: " + textPart.getText());
                    }
                }
            }
            System.out.println();
            
            // Example 3: Translate Chinese to English
            System.out.println("=== Translating Chinese to English ===");
            
            TextPart chineseTextPart = new TextPart("你好，世界！欢迎使用AI翻译机器人。", null);

            Message chineseMessage = new Message(
                    Message.Role.USER,
                    List.of(chineseTextPart),     // parts
                UUID.randomUUID().toString(),  // messageId
                null,                         // contextId
                null,                         // taskId
                null,                         // referenceTaskIds
                null,                          // metadata
                null,
                Message.MESSAGE
            );

            
            TaskSendParams chineseParams = new TaskSendParams(
                "chinese-to-english-task",
                null,  // sessionId
                chineseMessage,
                null,  // pushNotification
                null,  // historyLength
                Map.of()  // metadata
            );
            
            JSONRPCResponse chineseResponse = client.sendTask(chineseParams);
            Task chineseTask = (Task) chineseResponse.result();
            System.out.println("Original Chinese: " + chineseTextPart.getText());
            System.out.println("Task ID: " + chineseTask.getId());
            System.out.println("Translation Status: " + chineseTask.getStatus().state());
            
            // Print translation result if available in history
            if (chineseTask.getHistory() != null && chineseTask.getHistory().size() > 1) {
                Message lastMessage = chineseTask.getHistory().get(chineseTask.getHistory().size() - 1);
                if (lastMessage.getRole().equals("assistant") && !lastMessage.getParts().isEmpty()) {
                    Part translationPart = lastMessage.getParts().get(0);
                    if (translationPart instanceof TextPart textPart) {
                        System.out.println("English Translation: " + textPart.getText());
                    }
                }
            }
            System.out.println();
            
            // Example 4: Translate with streaming (French to English)
            System.out.println("=== Streaming Translation (French to English) ===");
            
            TextPart frenchTextPart = new TextPart("Bonjour le monde! Comment allez-vous?", null);
            Message frenchMessage = new Message(
                    Message.Role.USER,
                    List.of(frenchTextPart),     // parts
                    UUID.randomUUID().toString(),  // messageId
                    null,                         // contextId
                    null,                         // taskId
                    null,                         // referenceTaskIds
                    null                          // metadata
                    ,null,Message.MESSAGE
            );


            TaskSendParams frenchParams = new TaskSendParams(
                "french-streaming-task",
                null,  // sessionId
                frenchMessage,
                null,  // pushNotification
                null,  // historyLength
                Map.of()  // metadata
            );
            
            CountDownLatch streamingLatch = new CountDownLatch(1);
            System.out.println("Original French: " + frenchTextPart.getText());
            
            client.sendTaskStreaming(frenchParams, new StreamingEventListener() {
                @Override
                public void onEvent(Object event) {
                    System.out.println("Streaming translation event: " + event);
                }
                
                @Override
                public void onError(Exception exception) {
                    System.err.println("Translation streaming error: " + exception.getMessage());
                    streamingLatch.countDown();
                }
                
                @Override
                public void onComplete() {
                    System.out.println("Translation streaming completed");
                    streamingLatch.countDown();
                }
            });
            
            // Wait for streaming to complete
            if (streamingLatch.await(30, TimeUnit.SECONDS)) {
                System.out.println("Streaming translation finished successfully");
            } else {
                System.out.println("Translation streaming timed out");
            }
            System.out.println();
            
            // Example 5: Get task status for translation
            System.out.println("=== Getting Translation Task Status ===");
            TaskQueryParams queryParams = new TaskQueryParams(frenchToChineseTask.getId());
            JSONRPCResponse getResponse = client.getTask(queryParams);
            Task retrievedTask = (Task) getResponse.result();
            System.out.println("Retrieved translation task: " + retrievedTask.getId());
            System.out.println("Final status: " + retrievedTask.getStatus().state());
            System.out.println();
            
            // Example 6: Cancel a translation task
            System.out.println("=== Canceling Translation Task ===");
            
            TextPart cancelTextPart = new TextPart("Diese Übersetzung wird abgebrochen.", null); // German
            Message cancelMessage = new Message(
                    Message.Role.USER,
                    List.of(cancelTextPart),     // parts
                    UUID.randomUUID().toString(),  // messageId
                    null,                         // contextId
                    null,                         // taskId
                    null,                         // referenceTaskIds
                    null                          // metadata
                    ,null,Message.MESSAGE
            );
            
            TaskSendParams cancelParams = new TaskSendParams(
                "german-cancel-task",
                null,  // sessionId
                cancelMessage,
                null,  // pushNotification
                null,  // historyLength
                Map.of()  // metadata
            );
            
            // Send task to be canceled
            JSONRPCResponse cancelResponse = client.sendTask(cancelParams);
            Task cancelTask = (Task) cancelResponse.result();
            System.out.println("German text to translate: " + cancelTextPart.getText());
            System.out.println("Translation task to cancel: " + cancelTask.getId());
            
            // Cancel the task
            TaskIdParams cancelTaskParams = new TaskIdParams(cancelTask.getId(), Map.of());
            JSONRPCResponse cancelResult = client.cancelTask(cancelTaskParams);
            Task canceledTask = (Task) cancelResult.result();
            System.out.println("Task canceled: " + canceledTask.getId());
            System.out.println("Final status: " + canceledTask.getStatus().state());
            
        } catch (A2AClientException e) {
            System.err.println("A2A Translation Client Error: " + e.getMessage());
            if (e.getErrorCode() != null) {
                System.err.println("Error Code: " + e.getErrorCode());
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected translation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 