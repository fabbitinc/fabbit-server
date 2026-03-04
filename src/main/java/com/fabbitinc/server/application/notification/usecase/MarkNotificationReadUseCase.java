package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final AuthTokenParser authTokenParser;
    private final NotificationService notificationService;

    @Transactional
    public void execute(String authorizationHeader, UUID notificationId) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        notificationService.markAsRead(auth.userId(), notificationId);
    }
}
