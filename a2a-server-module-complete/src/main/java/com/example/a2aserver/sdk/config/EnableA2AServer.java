//package com.example.a2aserver.sdk.config;
//
//import java.lang.annotation.Documented;
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//
//import org.springframework.context.annotation.Import;
//
///**
// * 启用 A2A 服务器的注解
// *
// * 用户只需在配置类上添加此注解，即可启用 A2A 服务器
// */
//@Target(ElementType.TYPE)
//@Retention(RetentionPolicy.RUNTIME)
//@Documented
//@Import(A2AServerAutoConfiguration.class)
//public @interface EnableA2AServer {
//
//    /**
//     * 是否启用 REST 协议
//     */
//    boolean enableRest() default true;
//
//    /**
//     * 是否启用 gRPC 协议
//     */
//    boolean enableGrpc() default true;
//
//    /**
//     * 是否启用 JSON-RPC 协议
//     */
//    boolean enableJsonRpc() default true;
//
//    /**
//     * REST 服务器端口
//     */
//    int restPort() default 7003;
//
//    /**
//     * gRPC 服务器端口
//     */
//    int grpcPort() default 9092;
//
//    /**
//     * JSON-RPC 服务器端口
//     */
//    int jsonRpcPort() default 7003;
//}
