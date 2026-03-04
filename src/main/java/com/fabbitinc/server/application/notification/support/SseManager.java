package com.fabbitinc.server.application.notification.support;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class SseManager {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<BlockingQueue<String>>> connections =
            new ConcurrentHashMap<>();

    public BlockingQueue<String> connect(UUID userId) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        connections.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(queue);
        return queue;
    }

    public void disconnect(UUID userId, BlockingQueue<String> queue) {
        List<BlockingQueue<String>> queues = connections.get(userId);
        if (queues == null) {
            return;
        }
        queues.remove(queue);
        if (queues.isEmpty()) {
            connections.remove(userId);
        }
    }

    public void push(UUID userId, String data) {
        List<BlockingQueue<String>> queues = connections.get(userId);
        if (queues == null) {
            return;
        }
        for (BlockingQueue<String> queue : queues) {
            queue.offer(data);
        }
    }
}
