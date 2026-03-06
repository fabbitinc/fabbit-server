package com.fabbitinc.server.application.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;

@Schema(description = "응답 DTO")
public record MentionPayloadResponse(
        String projectId,
        String sourceIssueId,
        Integer sourceNumber,
        String sourceTitle,
        NotificationSourceIssueType sourceIssueType,
        Boolean isComment
) {
}
