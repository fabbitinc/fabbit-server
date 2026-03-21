package com.fabbitinc.server.application.chat.support;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public interface ChatSsePublisher {

    BlockingQueue<String> connect(UUID runId);

    void disconnect(UUID runId, BlockingQueue<String> queue);

    void push(UUID runId, String data);
}
