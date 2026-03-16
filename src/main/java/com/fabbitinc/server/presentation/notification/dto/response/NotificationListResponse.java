package com.fabbitinc.server.presentation.notification.dto.response;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;

import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record NotificationListResponse(
        List<NotificationItemResponse> items,
        UUID nextCursor,
        Map<String, NotificationUserSummaryResponse> users
) {
    public record NotificationItemResponse(
            UUID id,
            NotificationType type,
            UUID actorId,
            MentionPayloadResponse payload,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record MentionPayloadResponse(
            String projectId,
            String sourceIssueId,
            Integer sourceNumber,
            String sourceTitle,
            NotificationSourceIssueType sourceIssueType,
            Boolean isComment
    ) {
    }

    public record NotificationUserSummaryResponse(
            UUID userId,
            String fullName,
            String email,
            String phone,
            String profileImageUrl
    ) {
    }
}
