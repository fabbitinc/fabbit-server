package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarkAllNotificationsReadUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final NotificationService notificationService;

    @Transactional
    public void execute() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        notificationService.markAllAsRead(auth.userId());
    }
}
