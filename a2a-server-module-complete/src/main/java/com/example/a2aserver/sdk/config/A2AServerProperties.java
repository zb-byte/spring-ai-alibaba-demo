package com.example.a2aserver.sdk.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.a2aserver.sdk.protocol.ProtocolType;

/**
 * A2A 服务器配置属性
 */
@ConfigurationProperties(prefix = "a2a.server")
public class A2AServerProperties {

    /**
     * 是否启用 REST 服务器
     */
    private boolean restEnabled = true;

    /**
     * REST 服务器端口
     */
    private int restPort = 7003;

    /**
     * 是否启用 gRPC 服务器
     */
    private boolean grpcEnabled = true;

    /**
     * gRPC 服务器端口
     */
    private int grpcPort = 9092;

    /**
     * 是否启用 JSON-RPC 服务器
     */
    private boolean jsonRpcEnabled = true;

    /**
     * JSON-RPC 服务器端口（与 REST 共用 HTTP 端口）
     */
    private int jsonRpcPort = 7003;

    /**
     * 服务器主机
     */
    private String host = "localhost";

    /**
     * 自定义配置
     */
    private Map<String, Object> customConfig = new HashMap<>();

    public boolean isRestEnabled() {
        return restEnabled;
    }

    public void setRestEnabled(boolean restEnabled) {
        this.restEnabled = restEnabled;
    }

    public int getRestPort() {
        return restPort;
    }

    public void setRestPort(int restPort) {
        this.restPort = restPort;
    }

    public boolean isGrpcEnabled() {
        return grpcEnabled;
    }

    public void setGrpcEnabled(boolean grpcEnabled) {
        this.grpcEnabled = grpcEnabled;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public boolean isJsonRpcEnabled() {
        return jsonRpcEnabled;
    }

    public void setJsonRpcEnabled(boolean jsonRpcEnabled) {
        this.jsonRpcEnabled = jsonRpcEnabled;
    }

    public int getJsonRpcPort() {
        return jsonRpcPort;
    }

    public void setJsonRpcPort(int jsonRpcPort) {
        this.jsonRpcPort = jsonRpcPort;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, Object> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(Map<String, Object> customConfig) {
        this.customConfig = customConfig;
    }

    /**
     * 检查指定协议是否启用
     */
    public boolean isProtocolEnabled(ProtocolType protocolType) {
        return switch (protocolType) {
            case HTTP_REST -> restEnabled;
            case GRPC -> grpcEnabled;
            case JSON_RPC -> jsonRpcEnabled;
        };
    }

    /**
     * 获取指定协议的端口
     */
    public int getProtocolPort(ProtocolType protocolType) {
        return switch (protocolType) {
            case HTTP_REST -> restPort;
            case GRPC -> grpcPort;
            case JSON_RPC -> jsonRpcPort;
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final A2AServerProperties properties = new A2AServerProperties();

        public Builder enableRest(boolean enabled) {
            properties.setRestEnabled(enabled);
            return this;
        }

        public Builder restPort(int port) {
            properties.setRestPort(port);
            return this;
        }

        public Builder enableGrpc(boolean enabled) {
            properties.setGrpcEnabled(enabled);
            return this;
        }

        public Builder grpcPort(int port) {
            properties.setGrpcPort(port);
            return this;
        }

        public Builder enableJsonRpc(boolean enabled) {
            properties.setJsonRpcEnabled(enabled);
            return this;
        }

        public Builder jsonRpcPort(int port) {
            properties.setJsonRpcPort(port);
            return this;
        }

        public Builder host(String host) {
            properties.setHost(host);
            return this;
        }

        public A2AServerProperties build() {
            return properties;
        }
    }
}
