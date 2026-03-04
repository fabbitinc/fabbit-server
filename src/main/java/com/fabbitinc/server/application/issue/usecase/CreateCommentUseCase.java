package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.CreateCommentRequest;
import com.fabbitinc.server.application.issue.dto.response.CommentResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.domain.issue.model.IssueComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateCommentUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommentResponse execute(
            String authorizationHeader,
            IssueTargetType targetType,
            int issueNumber,
            CreateCommentRequest request
    ) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        UUID issueId = resolveIssueId(targetType, issueNumber);

        IssueComment comment = issueService.createComment(auth.userId(), issueId, request.body());
        return toResponse(comment);
    }

    private UUID resolveIssueId(IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }

    private CommentResponse toResponse(IssueComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getIssueId(),
                parseJson(comment.getBody()),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isModified(comment.getCreatedAt(), comment.getUpdatedAt()),
                comment.getCreatedBy()
        );
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private boolean isModified(Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(createdAt);
    }
}
