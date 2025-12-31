package com.example.a2aserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A gRPC Server Demo Application
 * 
 * 基于 A2A Java SDK v0.3.3.Final 构建的最小可运行 gRPC Server
 * 实现了一个简单的 Echo Agent，接收消息并返回 "Echo: " + 原消息
 */
@SpringBootApplication
public class A2aServerModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aServerModuleApplication.class, args);
    }
}

