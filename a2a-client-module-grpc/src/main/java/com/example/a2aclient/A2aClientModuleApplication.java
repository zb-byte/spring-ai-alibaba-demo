package com.example.a2aclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A gRPC Client Demo Application
 * 
 * 基于 A2A Java SDK v0.3.3.Final 构建的 gRPC Client
 * 用于连接和测试 A2A Echo Agent Server
 */
@SpringBootApplication
public class A2aClientModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aClientModuleApplication.class, args);
    }
}

