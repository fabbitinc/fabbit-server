package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarkAllNotificationsReadUseCase {

    private final AuthTokenParser authTokenParser;
    private final NotificationService notificationService;

    @Transactional
    public void execute(String authorizationHeader) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        notificationService.markAllAsRead(auth.userId());
    }
}
