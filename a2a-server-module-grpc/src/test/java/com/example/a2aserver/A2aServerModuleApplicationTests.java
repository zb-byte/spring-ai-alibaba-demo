package com.example.a2aserver;

import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class A2aServerModuleApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能够正常加载
    }

    @Test
    void agentCardEndpointReturnsValidCard() {
        // 测试 Agent Card 端点
        ResponseEntity<AgentCard> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/.well-known/agent-card.json",
                AgentCard.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        AgentCard agentCard = response.getBody();
        assertThat(agentCard.name()).isEqualTo("Echo Agent");
        assertThat(agentCard.description()).contains("Echo Agent");
        assertThat(agentCard.version()).isEqualTo("1.0.0");
        assertThat(agentCard.capabilities().streaming()).isTrue();
        assertThat(agentCard.skills()).hasSize(1);
        assertThat(agentCard.skills().get(0).id()).isEqualTo("echo");
    }
}