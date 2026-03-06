package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.notification.service.NotificationService;
import com.fabbitinc.server.application.notification.usecase.command.MarkNotificationReadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MarkNotificationReadUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final NotificationService notificationService;

    public void execute(MarkNotificationReadCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        notificationService.markAsRead(auth.userId(), command.notificationId());
    }
}
