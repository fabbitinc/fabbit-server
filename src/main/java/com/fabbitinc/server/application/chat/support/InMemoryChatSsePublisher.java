package com.fabbitinc.server.application.chat.support;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class InMemoryChatSsePublisher implements ChatSsePublisher {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<BlockingQueue<String>>> connections =
            new ConcurrentHashMap<>();

    @Override
    public BlockingQueue<String> connect(UUID runId) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        connections.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(queue);
        return queue;
    }

    @Override
    public void disconnect(UUID runId, BlockingQueue<String> queue) {
        List<BlockingQueue<String>> queues = connections.get(runId);
        if (queues == null) {
            return;
        }
        queues.remove(queue);
        if (queues.isEmpty()) {
            connections.remove(runId);
        }
    }

    @Override
    public void push(UUID runId, String data) {
        List<BlockingQueue<String>> queues = connections.get(runId);
        if (queues == null) {
            return;
        }
        for (BlockingQueue<String> queue : queues) {
            queue.offer(data);
        }
    }
}
