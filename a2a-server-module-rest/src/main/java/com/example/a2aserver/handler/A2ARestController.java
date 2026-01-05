package com.example.a2aserver.handler;

import java.io.IOException;
import java.util.concurrent.Flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.transport.rest.handler.RestHandler;

@RestController
@RequestMapping
public class A2ARestController {

    private static final Logger logger = LoggerFactory.getLogger(A2ARestController.class);

    private final RestHandler restHandler;

    public A2ARestController(AgentCard agentCard,
                            RequestHandler requestHandler,
                            @Qualifier("a2aExecutor") Executor executor) {
        this.restHandler = new RestHandler(agentCard, requestHandler, executor);
        logger.info("A2ARestController initialized");
    }

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAgentCard() {
        try {
            RestHandler.HTTPRestResponse response = restHandler.getAgentCard();
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.parseMediaType(response.getContentType()))
                    .body(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting agent card", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/message:send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendMessage(@RequestBody String body) {
        try {
            ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
            RestHandler.HTTPRestResponse response = restHandler.sendMessage(body, context);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.parseMediaType(response.getContentType()))
                    .body(response.getBody());
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/message:stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStreamingMessage(@RequestBody String body) {
        SseEmitter emitter = new SseEmitter(30000L);
        
        try {
            ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
            RestHandler.HTTPRestResponse response = restHandler.sendStreamingMessage(body, context);
            
            if (response instanceof RestHandler.HTTPRestStreamingResponse streamingResponse) {
                Flow.Publisher<String> publisher = streamingResponse.getPublisher();
                
                publisher.subscribe(new Flow.Subscriber<String>() {
                    private Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String item) {
                        try {
                            emitter.send(SseEmitter.event().data(item));
                        } catch (IOException e) {
                            logger.error("Error sending SSE event", e);
                            if (this.subscription != null) {
                                this.subscription.cancel();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Streaming error", throwable);
                        emitter.completeWithError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.complete();
                    }
                });
            } else {
                emitter.completeWithError(new IllegalStateException("Expected streaming response"));
            }
        } catch (Exception e) {
            logger.error("Error sending streaming message", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @GetMapping(value = "/v1/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTask(@PathVariable String taskId,
                                         @RequestParam(required = false, defaultValue = "0") int historyLength) {
        try {
            ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
            RestHandler.HTTPRestResponse response = restHandler.getTask(taskId, historyLength, context);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.parseMediaType(response.getContentType()))
                    .body(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting task", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/tasks/{taskId}:cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelTask(@PathVariable String taskId) {
        try {
            ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
            RestHandler.HTTPRestResponse response = restHandler.cancelTask(taskId, context);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.parseMediaType(response.getContentType()))
                    .body(response.getBody());
        } catch (Exception e) {
            logger.error("Error cancelling task", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

