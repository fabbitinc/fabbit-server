package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final NotificationService notificationService;

    @Transactional
    public void execute(UUID notificationId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        notificationService.markAsRead(auth.userId(), notificationId);
    }
}
