package com.example.a2aserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Complete A2A Server Application
 *
 * 使用 A2A Server SDK，支持三种协议：
 * - REST: HTTP REST API (端口 7003)
 * - gRPC: 基于 gRPC 的高性能 RPC (端口 9092)
 * - JSON-RPC: 基于 JSON-RPC 的通信协议 (端口 7003)
 *
 * 用户只需实现 A2AAgent 接口，SDK 会自动处理所有协议细节
 *
 * @author A2A Team
 */
@SpringBootApplication
public class CompleteA2AServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompleteA2AServerApplication.class, args);
    }
}
