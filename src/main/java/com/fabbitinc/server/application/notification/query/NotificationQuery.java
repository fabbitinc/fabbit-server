package com.fabbitinc.server.application.notification.query;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.notification.query.condition.NotificationListCondition;
import com.fabbitinc.server.application.notification.query.result.NotificationListResult;
import com.fabbitinc.server.application.notification.query.result.UnreadCountResult;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;
    private final EntityManager entityManager;

    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern NUMBER_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");
    private static final Pattern BOOLEAN_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");

    public NotificationListResult list(NotificationListCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID cursor = condition.cursor();
        int limit = condition.limit();
        boolean unreadOnly = condition.unreadOnly();
        PathBuilder<Notification> notification = new PathBuilder<>(Notification.class, "notification");
        var notificationIdExpr = notification.getComparable("id", UUID.class);

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(notification.get("userId", UUID.class).eq(auth.userId()));
        if (cursor != null) {
            predicate.and(notificationIdExpr.lt(cursor));
        }
        if (unreadOnly) {
            predicate.and(notification.getDateTime("readAt", Instant.class).isNull());
        }

        List<Notification> notifications = queryFactory()
                .selectFrom(notification)
                .where(predicate)
                .orderBy(notificationIdExpr.desc())
                .limit(limit)
                .fetch();

        List<NotificationListResult.NotificationItemResult> items = notifications.stream()
                .map(this::toNotificationItemResult)
                .toList();
        UUID nextCursor = notifications.size() == limit
                ? notifications.get(notifications.size() - 1).getId()
                : null;

        Set<UUID> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .collect(java.util.stream.Collectors.toSet());
        List<User> users = actorIds.isEmpty()
                ? List.of()
                : userRepository.findByIdInOrderByFullNameAsc(actorIds);
        Map<String, NotificationListResult.NotificationUserSummaryResult> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(
                    user.getId().toString(),
                    new NotificationListResult.NotificationUserSummaryResult(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    )
            );
        }

        return new NotificationListResult(items, nextCursor, userMap);
    }

    public UnreadCountResult getUnreadCount() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(auth.userId());
        return new UnreadCountResult((int) unreadCount);
    }

    private NotificationListResult.NotificationItemResult toNotificationItemResult(Notification notification) {
        return new NotificationListResult.NotificationItemResult(
                notification.getId(),
                notification.getType(),
                notification.getActorId(),
                parseMentionPayload(notification.getPayload()),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private NotificationListResult.MentionPayloadResult parseMentionPayload(String payload) {
        return new NotificationListResult.MentionPayloadResult(
                extractString(payload, "project_id"),
                extractString(payload, "source_issue_id"),
                extractInteger(payload, "source_number"),
                extractString(payload, "source_title"),
                extractSourceIssueType(payload, "source_issue_type"),
                extractBoolean(payload, "is_comment")
        );
    }

    private NotificationSourceIssueType extractSourceIssueType(String payload, String field) {
        String rawValue = extractString(payload, field);
        if (rawValue == null) {
            return null;
        }
        try {
            return NotificationSourceIssueType.from(rawValue);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 payload source_issue_type 값이 유효하지 않습니다");
        }
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

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
