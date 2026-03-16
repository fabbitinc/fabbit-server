package com.fabbitinc.server.application.workitem.usecase;

import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class WorkItemUseCaseSupport {

    private WorkItemUseCaseSupport() {
    }

    public static UUID resolveIssueId(IssueService issueService, int issueNumber) {
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }

    public static UUID resolveEngineeringChangeId(
            EngineeringChangeService engineeringChangeService,
            int engineeringChangeNumber
    ) {
        return engineeringChangeService.getEngineeringChangeByNumberOrThrow(engineeringChangeNumber).getId();
    }

    public static CommentResult toCommentResult(AbstractComment comment, ObjectMapper objectMapper) {
        return new CommentResult(
                comment.getId(),
                comment.getTargetId(),
                parseJson(comment.getBody(), objectMapper),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isModified(comment.getCreatedAt(), comment.getUpdatedAt()),
                comment.getCreatedBy()
        );
    }

    public static SyncDiffResult toSyncDiffResult(IssueService.DiffResult diff) {
        return new SyncDiffResult(diff.added().size(), diff.removed().size());
    }

    public static SyncDiffResult toSyncDiffResult(EngineeringChangeService.DiffResult diff) {
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
