package com.fabbitinc.server.presentation.notification.dto.response;

import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;
import io.swagger.v3.oas.annotations.media.Schema;

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
