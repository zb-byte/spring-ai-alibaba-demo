package com.example.a2aserver.sdk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A2A 服务器配置属性绑定
 *
 * 从 application.yml 中读取配置
 */
@ConfigurationProperties(prefix = "a2a.server")
public class A2AServerPropertiesConfiguration {

    private boolean autoStart = true;
    private boolean restEnabled = true;
    private int restPort = 7003;
    private boolean grpcEnabled = true;
    private int grpcPort = 9092;
    private boolean jsonRpcEnabled = true;
    private int jsonRpcPort = 7003;
    private String host = "localhost";

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

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

    /**
     * 转换为 A2AServerProperties
     */
    public A2AServerProperties toProperties() {
        return A2AServerProperties.builder()
                .enableRest(restEnabled)
                .restPort(restPort)
                .enableGrpc(grpcEnabled)
                .grpcPort(grpcPort)
                .enableJsonRpc(jsonRpcEnabled)
                .jsonRpcPort(jsonRpcPort)
                .host(host)
                .build();
    }
}
