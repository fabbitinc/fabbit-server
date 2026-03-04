package com.fabbitinc.server.application.notification.usecase;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public record NotificationStreamSession(
        UUID userId,
        BlockingQueue<String> queue
) {
}
