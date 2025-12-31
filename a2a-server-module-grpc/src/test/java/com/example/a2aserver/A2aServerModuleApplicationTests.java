package com.example.a2aserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成测试
 * 
 * 注意：这些测试需要配置 OPENAI_API_KEY 环境变量才能运行
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class A2aServerModuleApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能够正常加载
    }
}
