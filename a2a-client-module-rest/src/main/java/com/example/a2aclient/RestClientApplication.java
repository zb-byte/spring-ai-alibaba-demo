package com.example.a2aclient;

import com.example.a2aclient.service.RestDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * A2A HTTP REST Client Demo Application
 * 基于 A2A Java SDK v0.3.3.Final 构建的 HTTP REST Client
 */
@SpringBootApplication
public class RestClientApplication {

    private static final Logger logger = LoggerFactory.getLogger(RestClientApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(RestClientApplication.class, args);
        RestDemo restDemo = context.getBean(RestDemo.class);
        
        try {
            logger.info("=========================================");
            logger.info("A2A Client Test Suite");
            logger.info("=========================================");
            
            // 测试获取 Agent Card
            logger.info("\n[Test 1] Fetching Agent Card...");
            restDemo.getAgentCard();
            Thread.sleep(500); // 短暂等待，让日志更清晰
            
            // 测试发送同步消息
            logger.info("\n[Test 2] Sending Synchronous Message...");
            restDemo.sendMessage("你好，请介绍一下你自己");
            Thread.sleep(500);
            
            // 测试发送流式消息
            logger.info("\n[Test 3] Sending Streaming Message...");
            logger.info("Streaming response (real-time):");
            restDemo.sendMessageStreamingForConsole("请写一首关于春天的诗");
            Thread.sleep(500);
            
            logger.info("\n=========================================");
            logger.info("All tests completed!");
            logger.info("=========================================");
            
        } catch (InterruptedException e) {
            logger.error("Test interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Test failed", e);
        } finally {
            System.exit(SpringApplication.exit(context, () -> 0));
        }
    }
}
