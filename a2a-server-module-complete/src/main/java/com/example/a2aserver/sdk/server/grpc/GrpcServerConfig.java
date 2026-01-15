package com.example.a2aserver.sdk.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 服务器配置
 * 
 * 启动和管理 gRPC 服务器
 */
@Configuration
public class GrpcServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServerConfig.class);

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    private final A2AGrpcService a2aGrpcService;
    private Server server;

    public GrpcServerConfig(A2AGrpcService a2aGrpcService) {
        this.a2aGrpcService = a2aGrpcService;
    }

    @PostConstruct
    public void startGrpcServer() throws IOException {
        server = ServerBuilder.forPort(grpcPort)
                .addService(a2aGrpcService)
                .addService(ProtoReflectionService.newInstance())  // 支持 gRPC 反射
                .build()
                .start();

        logger.info("===========================================");
        logger.info("🚀 A2A gRPC Server started on port: {}", grpcPort);
        logger.info("===========================================");

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server...");
            try {
                stopGrpcServer();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error shutting down gRPC server", e);
            }
        }));
    }

    @PreDestroy
    public void stopGrpcServer() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            logger.info("gRPC server stopped");
        }
    }

    public int getGrpcPort() {
        return grpcPort;
    }
}
