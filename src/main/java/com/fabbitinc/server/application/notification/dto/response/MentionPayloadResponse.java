package com.fabbitinc.server.application.notification.dto.response;

import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;

public record MentionPayloadResponse(
        String projectId,
        String sourceIssueId,
        Integer sourceNumber,
        String sourceTitle,
        NotificationSourceIssueType sourceIssueType,
        Boolean isComment
) {
}
