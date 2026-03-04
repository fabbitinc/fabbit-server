package com.fabbitinc.server.application.notification.dto.response;

public record MentionPayloadResponse(
        String projectId,
        String sourceIssueId,
        Integer sourceNumber,
        String sourceTitle,
        String sourceIssueType,
        Boolean isComment
) {
}
