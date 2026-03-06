package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.notification.support.SseManager;
import com.fabbitinc.server.application.notification.usecase.result.NotificationStreamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationStreamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SseManager sseManager;

    public NotificationStreamResult execute() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        BlockingQueue<String> queue = sseManager.connect(auth.userId());
        return new NotificationStreamResult(auth.userId(), queue);
    }

    public void disconnect(NotificationStreamResult result) {
        sseManager.disconnect(result.userId(), result.queue());
    }
}
