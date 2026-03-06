package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.result.CommentResult;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import com.fabbitinc.server.domain.issue.model.IssueComment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

final class IssueUseCaseSupport {

    private IssueUseCaseSupport() {
    }

    static UUID resolveIssueId(IssueService issueService, IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }

    static CommentResult toCommentResult(IssueComment comment, ObjectMapper objectMapper) {
        return new CommentResult(
                comment.getId(),
                comment.getIssueId(),
                parseJson(comment.getBody(), objectMapper),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isModified(comment.getCreatedAt(), comment.getUpdatedAt()),
                comment.getCreatedBy()
        );
    }

    static SyncDiffResult toSyncDiffResult(IssueService.DiffResult diff) {
        return new SyncDiffResult(diff.added().size(), diff.removed().size());
    }

    private static JsonNode parseJson(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private static boolean isModified(Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(createdAt);
    }
}
