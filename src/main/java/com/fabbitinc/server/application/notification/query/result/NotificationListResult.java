package com.fabbitinc.server.application.notification.query.result;

import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationListResult(
        List<NotificationItemResult> items,
        UUID nextCursor,
        Map<String, NotificationUserSummaryResult> users
) {
    public record NotificationItemResult(
            UUID id,
            NotificationType type,
            UUID actorId,
            MentionPayloadResult payload,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record MentionPayloadResult(
            String projectId,
            String sourceIssueId,
            Integer sourceNumber,
            String sourceTitle,
            NotificationSourceIssueType sourceIssueType,
            Boolean isComment
    ) {
    }

    public record NotificationUserSummaryResult(
            UUID userId,
            String fullName,
            String email,
            String phone,
            String profileImageUrl
    ) {
    }
}
