package com.example.a2aserver.sdk.protocol.impl;

import java.util.List;

import com.example.a2aserver.sdk.config.A2AServerProperties;
import org.springframework.context.ApplicationContext;

import com.example.a2aserver.sdk.agent.A2AAgent;
import com.example.a2aserver.sdk.protocol.ProtocolType;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.a2a.spec.AgentCard;
import io.a2a.grpc.AgentCapabilities;
import io.a2a.grpc.AgentSkill;

/**
 * gRPC 协议服务器实现
 *
 * 注意：此类通过工厂创建，不使用 Spring Bean 管理
 */
public class GrpcProtocolServer extends AbstractProtocolServer {

    private Server grpcServer;
    private final A2AGrpcServiceDelegate grpcServiceDelegate;

    public GrpcProtocolServer(A2AAgent<?> agent,
                             ApplicationContext applicationContext,
                             A2AServerProperties properties) {
        super(agent, applicationContext, properties);
        this.port = properties.getGrpcPort();
        // 创建完整的 gRPC AgentCard
        io.a2a.grpc.AgentCard grpcAgentCard = createGrpcAgentCard();
        this.grpcServiceDelegate = new A2AGrpcServiceDelegate(agent, grpcAgentCard);
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.GRPC;
    }

    @Override
    protected AgentCard buildAgentCard() {
        // gRPC 使用自己的 AgentCard
        return null;
    }

    @Override
    protected void doStart(AgentCard agentCard) throws Exception {
        grpcServer = ServerBuilder.forPort(port)
                .addService(grpcServiceDelegate)
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();

        logger.info("gRPC server started on port {}", port);
    }

    @Override
    protected void doStop() throws Exception {
        if (grpcServer != null) {
            grpcServer.shutdown();
            grpcServer.awaitTermination();
        }
    }

    /**
     * 创建 gRPC AgentCard
     */
    public io.a2a.grpc.AgentCard createGrpcAgentCard() {
        AgentCapabilities capabilities = AgentCapabilities.newBuilder()
                .setStreaming(agent.supportsStreaming())
                .setPushNotifications(false)
                .build();

        List<AgentSkill> skills = List.of(
                AgentSkill.newBuilder()
                        .setId("chat")
                        .setName("Chat")
                        .setDescription("Chat with the agent")
                        .build()
        );

        return io.a2a.grpc.AgentCard.newBuilder()
                .setName(agent.getName() + " (gRPC)")
                .setDescription(agent.getDescription())
                .setUrl(getServerUrl())
                .setVersion(agent.getVersion())
                .setCapabilities(capabilities)
                .addDefaultInputModes("text")
                .addDefaultOutputModes("text")
                .addAllSkills(skills)
                .build();
    }
}
