package com.fabbitinc.server.application.notification.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.notification.dto.response.MentionPayloadResponse;
import com.fabbitinc.server.application.notification.dto.response.NotificationListResponse;
import com.fabbitinc.server.application.notification.dto.response.NotificationResponse;
import com.fabbitinc.server.application.notification.dto.response.NotificationUserSummaryResponse;
import com.fabbitinc.server.application.notification.dto.response.UnreadCountResponse;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class NotificationQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern NUMBER_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");
    private static final Pattern BOOLEAN_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");

    @Transactional(readOnly = true)
    public NotificationListResponse listNotifications(UUID cursor,
            int limit,
            boolean unreadOnly
    ) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<Notification> notifications = notificationRepository.listByUserCursor(
                auth.userId(),
                cursor,
                unreadOnly,
                PageRequest.of(0, limit)
        );

        List<NotificationResponse> items = notifications.stream()
                .map(this::toNotificationResponse)
                .toList();
        UUID nextCursor = notifications.size() == limit
                ? notifications.get(notifications.size() - 1).getId()
                : null;

        Set<UUID> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .collect(java.util.stream.Collectors.toSet());
        List<User> users = actorIds.isEmpty()
                ? List.of()
                : userRepository.findAllByIdInOrderByFullName(actorIds);
        Map<String, NotificationUserSummaryResponse> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(
                    user.getId().toString(),
                    new NotificationUserSummaryResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    )
            );
        }

        return new NotificationListResponse(items, nextCursor, userMap);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(auth.userId());
        return new UnreadCountResponse((int) unreadCount);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getActorId(),
                parseMentionPayload(notification.getPayload()),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private MentionPayloadResponse parseMentionPayload(String payload) {
        return new MentionPayloadResponse(
                extractString(payload, "project_id"),
                extractString(payload, "source_issue_id"),
                extractInteger(payload, "source_number"),
                extractString(payload, "source_title"),
                extractString(payload, "source_issue_type"),
                extractBoolean(payload, "is_comment")
        );
    }

    private String extractString(String payload, String field) {
        if (payload == null) {
            return null;
        }
        Matcher matcher = compile(STRING_FIELD_PATTERN, field).matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private Integer extractInteger(String payload, String field) {
        if (payload == null) {
            return null;
        }
        Matcher matcher = compile(NUMBER_FIELD_PATTERN, field).matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    private Boolean extractBoolean(String payload, String field) {
        if (payload == null) {
            return null;
        }
        Matcher matcher = compile(BOOLEAN_FIELD_PATTERN, field).matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return Boolean.valueOf(matcher.group(1));
    }

    private Pattern compile(Pattern template, String field) {
        return Pattern.compile(String.format(template.pattern(), Pattern.quote(field)));
    }
}
