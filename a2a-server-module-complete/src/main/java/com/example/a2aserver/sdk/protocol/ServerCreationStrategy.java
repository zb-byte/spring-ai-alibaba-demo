package com.example.a2aserver.sdk.protocol;

/**
 * 服务器创建策略
 *
 * 定义协议服务器是通过 Spring Bean 管理还是通过工厂创建
 */
public enum ServerCreationStrategy {
    /**
     * 通过 Spring 容器管理
     */
    SPRING_BEAN,
    
    /**
     * 通过工厂创建
     */
    FACTORY
}
