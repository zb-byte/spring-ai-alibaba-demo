package com.example.a2aserver.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.a2aserver.agent.A2AAgentExecutor;

import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.config.A2AConfigProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Configuration
public class A2AServerConfig {

    @Bean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    public QueueManager queueManager(TaskStore taskStore) {
        // InMemoryTaskStore implements TaskStateProvider, so we can use it directly
        // InMemoryQueueManager is the SDK's built-in implementation
        return new InMemoryQueueManager((io.a2a.server.tasks.TaskStateProvider) taskStore);
    }

    @Bean
    public PushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    public PushNotificationSender pushNotificationSender(PushNotificationConfigStore pushConfigStore) {
        return new BasePushNotificationSender(pushConfigStore);
    }

    @Bean
    public RequestHandler requestHandler(A2AAgentExecutor agentExecutor, 
                                        TaskStore taskStore, 
                                        QueueManager queueManager,
                                        PushNotificationConfigStore pushConfigStore,
                                        PushNotificationSender pushSender,
                                         @Qualifier("a2aExecutor") Executor executor) {
        return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, 
                                        pushConfigStore, pushSender, executor);
    }

    @Bean
    public Executor a2aExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public A2AConfigProvider a2aConfigProvider() {
        return new SpringA2AConfigProvider();
    }

    /**
     * Simple A2AConfigProvider implementation for Spring Boot
     * Loads default values from META-INF/a2a-defaults.properties
     */
    private static class SpringA2AConfigProvider implements A2AConfigProvider {
        private final Map<String, String> defaults = new HashMap<>();

        public SpringA2AConfigProvider() {
            loadDefaultsFromClasspath();
        }

        private void loadDefaultsFromClasspath() {
            try {
                Enumeration<URL> resources = Thread.currentThread()
                        .getContextClassLoader()
                        .getResources("META-INF/a2a-defaults.properties");

                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    Properties props = new Properties();
                    try (InputStream is = url.openStream()) {
                        props.load(is);
                        for (String key : props.stringPropertyNames()) {
                            defaults.put(key, props.getProperty(key));
                        }
                    }
                }
            } catch (IOException e) {
                // If no defaults file found, use hardcoded defaults
            }
            
            // Override timeout values for LLM API calls which can take longer
            // These values can be overridden by system properties or environment variables
            String agentTimeout = System.getProperty("a2a.blocking.agent.timeout.seconds");
            if (agentTimeout == null) {
                agentTimeout = System.getenv("A2A_BLOCKING_AGENT_TIMEOUT_SECONDS");
            }
            if (agentTimeout == null) {
                agentTimeout = "120"; // Default: 120 seconds for LLM calls
            }
            defaults.put("a2a.blocking.agent.timeout.seconds", agentTimeout);
            
            String consumptionTimeout = System.getProperty("a2a.blocking.consumption.timeout.seconds");
            if (consumptionTimeout == null) {
                consumptionTimeout = System.getenv("A2A_BLOCKING_CONSUMPTION_TIMEOUT_SECONDS");
            }
            if (consumptionTimeout == null) {
                consumptionTimeout = "120"; // Default: 120 seconds for LLM calls
            }
            defaults.put("a2a.blocking.consumption.timeout.seconds", consumptionTimeout);
            
            // Set other defaults only if not already loaded from classpath
            defaults.putIfAbsent("a2a.executor.core-pool-size", "5");
            defaults.putIfAbsent("a2a.executor.max-pool-size", "50");
            defaults.putIfAbsent("a2a.executor.keep-alive-seconds", "60");
        }

        @Override
        public String getValue(String name) {
            String value = defaults.get(name);
            if (value == null) {
                throw new IllegalArgumentException("No configuration value found for: " + name);
            }
            return value;
        }

        @Override
        public Optional<String> getOptionalValue(String name) {
            return Optional.ofNullable(defaults.get(name));
        }
    }
}

