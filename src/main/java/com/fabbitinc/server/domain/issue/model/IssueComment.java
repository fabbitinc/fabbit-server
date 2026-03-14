package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "issue_comments",
        indexes = {
                @Index(name = "ix_issue_comments_issue_id", columnList = "issue_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueComment extends AbstractActorAuditableEntity {

    public static final String CODE_ISSUE_COMMENT_ISSUE_REQUIRED = "ISSUE_COMMENT_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_COMMENT_BODY_REQUIRED = "ISSUE_COMMENT_BODY_REQUIRED";
    public static final String CODE_ISSUE_COMMENT_ACTOR_REQUIRED = "ISSUE_COMMENT_ACTOR_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    private IssueComment(UUID issueId, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.issueId = requireIssueId(issueId);
        this.body = requireBody(body);
        initializeActor(requireActorId(actorId));
    }

    public static IssueComment write(UUID issueId, String body, UUID actorId) {
        return new IssueComment(issueId, body, actorId);
    }

    public static IssueComment write(Issue issue, String body, UUID actorId) {
        if (issue == null) {
            throw new DomainException(CODE_ISSUE_COMMENT_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        IssueComment comment = new IssueComment(issue.getId(), body, actorId);
        comment.issue = issue;
        return comment;
    }

    public void updateBody(String body, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        String requiredBody = requireBody(body);
        mutate(requiredActorId, () -> this.body = requiredBody);
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_COMMENT_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    private String requireBody(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ISSUE_COMMENT_BODY_REQUIRED, "댓글 내용은 필수입니다");
        }
        return value;
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_COMMENT_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return value;
    }
}
