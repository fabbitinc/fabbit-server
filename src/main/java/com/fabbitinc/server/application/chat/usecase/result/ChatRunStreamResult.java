package com.fabbitinc.server.application.chat.usecase.result;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public record ChatRunStreamResult(
        UUID runId,
        BlockingQueue<String> queue
) {
}
