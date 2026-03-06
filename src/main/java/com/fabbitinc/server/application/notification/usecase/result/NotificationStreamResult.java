package com.fabbitinc.server.application.notification.usecase.result;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public record NotificationStreamResult(
        UUID userId,
        BlockingQueue<String> queue
) {
}
