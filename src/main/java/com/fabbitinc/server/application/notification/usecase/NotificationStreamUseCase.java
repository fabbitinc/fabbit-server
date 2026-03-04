package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.notification.support.SseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
@RequiredArgsConstructor
public class NotificationStreamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SseManager sseManager;

    public NotificationStreamSession connect() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        BlockingQueue<String> queue = sseManager.connect(auth.userId());
        return new NotificationStreamSession(auth.userId(), queue);
    }

    public void disconnect(NotificationStreamSession session) {
        sseManager.disconnect(session.userId(), session.queue());
    }
}
