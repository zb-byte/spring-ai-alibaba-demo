package com.example.a2aserver.sdk.protocol;

/**
 * 协议类型枚举
 */
public enum ProtocolType {

    /**
     * HTTP REST 协议
     */
    HTTP_REST("REST", "HTTP REST API", 7003),

    /**
     * gRPC 协议
     */
    GRPC("gRPC", "gRPC Protocol", 9092),

    /**
     * JSON-RPC 协议
     */
    JSON_RPC("JSON-RPC", "JSON-RPC Protocol", 7003);

    private final String code;
    private final String description;
    private final int defaultPort;

    ProtocolType(String code, String description, int defaultPort) {
        this.code = code;
        this.description = description;
        this.defaultPort = defaultPort;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}
