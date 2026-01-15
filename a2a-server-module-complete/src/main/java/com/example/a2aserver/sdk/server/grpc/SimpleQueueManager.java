package com.example.a2aserver.sdk.server.grpc;

import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.TaskStateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 简单的事件队列管理器
 * 
 * 管理任务的事件队列，支持创建、获取、关闭队列
 */
@Component
public class SimpleQueueManager implements QueueManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleQueueManager.class);
    
    private final ConcurrentMap<String, EventQueue> queues = new ConcurrentHashMap<>();
    private final TaskStateProvider taskStateProvider;
    
    public SimpleQueueManager() {
        this.taskStateProvider = new SimpleTaskStateProvider();
    }
    
    @Override
    public void add(String taskId, EventQueue queue) {
        EventQueue existing = queues.putIfAbsent(taskId, queue);
        if (existing != null) {
            throw new IllegalStateException("Queue already exists for task: " + taskId);
        }
        logger.debug("Added queue for task: {}", taskId);
    }
    
    @Override
    public EventQueue get(String taskId) {
        return queues.get(taskId);
    }
    
    @Override
    public EventQueue tap(String taskId) {
        // 在 v0.3.3.Final 中，tap() 方法不是公共的
        // 返回主队列本身，让调用者直接使用
        return queues.get(taskId);
    }
    
    @Override
    public void close(String taskId) {
        EventQueue queue = queues.remove(taskId);
        if (queue != null) {
            queue.close();
            logger.debug("Closed queue for task: {}", taskId);
        }
    }
    
    @Override
    public EventQueue createOrTap(String taskId) {
        EventQueue existing = queues.get(taskId);
        
        // 清理已关闭的队列
        if (existing != null && existing.isClosed()) {
            queues.remove(taskId);
            existing = null;
        }
        
        if (existing == null) {
            // 创建新队列
            EventQueue newQueue = getEventQueueBuilder(taskId).build();
            existing = queues.putIfAbsent(taskId, newQueue);
            if (existing == null) {
                existing = newQueue;
                logger.debug("Created new queue for task: {}", taskId);
            }
        }
        
        // 在 v0.3.3.Final 中，tap() 方法不是公共的
        // 返回主队列本身
        return existing;
    }
    
    @Override
    public void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException {
        eventQueue.awaitQueuePollerStart();
    }
    
    @Override
    public EventQueue.EventQueueBuilder getEventQueueBuilder(String taskId) {
        // 使用 QueueManager 接口的默认方法
        return QueueManager.super.getEventQueueBuilder(taskId)
                .taskId(taskId)
                .taskStateProvider(taskStateProvider);
    }
    
    @Override
    public int getActiveChildQueueCount(String taskId) {
        EventQueue queue = queues.get(taskId);
        if (queue == null || queue.isClosed()) {
            return -1;
        }
        return 0; // 简化实现
    }
    
    /**
     * 简单的任务状态提供者
     */
    private static class SimpleTaskStateProvider implements TaskStateProvider {
        private final ConcurrentMap<String, Boolean> finalizedTasks = new ConcurrentHashMap<>();
        
        @Override
        public boolean isTaskActive(String taskId) {
            // 如果任务没有被标记为已完成，则认为是活跃的
            return !finalizedTasks.getOrDefault(taskId, false);
        }
        
        @Override
        public boolean isTaskFinalized(String taskId) {
            return finalizedTasks.getOrDefault(taskId, false);
        }
        
        public void markFinalized(String taskId) {
            finalizedTasks.put(taskId, true);
        }
    }
}
