package com.example.a2aclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application class for a2a-client-module-grpc
 * This module uses a2a-java-sdk-client 0.3.3.Final with gRPC transport
 */
@SpringBootApplication
public class A2aClientModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aClientModuleApplication.class, args);
    }
}

