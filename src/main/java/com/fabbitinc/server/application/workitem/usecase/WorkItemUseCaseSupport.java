package com.fabbitinc.server.application.workitem.usecase;

import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import com.fabbitinc.server.application.workitem.usecase.result.CommentUserSummaryResult;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import java.time.Instant;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class WorkItemUseCaseSupport {

    private WorkItemUseCaseSupport() {
    }

    public static CommentResult toCommentResult(
            AbstractComment comment,
            ObjectMapper objectMapper,
            User createdBy,
            FileUrlResolver fileUrlResolver
    ) {
        return new CommentResult(
                comment.getId(),
                comment.getTargetId(),
                parseJson(comment.getBody(), objectMapper),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isModified(comment.getCreatedAt(), comment.getUpdatedAt()),
                toUserSummary(createdBy, fileUrlResolver)
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

    public static CommentUserSummaryResult toUserSummary(User user, FileUrlResolver fileUrlResolver) {
        if (user == null) {
            return null;
        }
        return new CommentUserSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }
}
