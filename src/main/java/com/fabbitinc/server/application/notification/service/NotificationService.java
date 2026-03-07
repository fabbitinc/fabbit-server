package com.fabbitinc.server.application.notification.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void markAsRead(UUID userId, UUID notificationId) {
        int updated = notificationRepository.markAsRead(userId, notificationId, Instant.now());
        if (updated == 0) {
            throw new AppException(ErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다");
        }
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }
}
