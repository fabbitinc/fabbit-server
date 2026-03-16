package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
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
public class IssueComment extends AbstractComment {

    public static final String CODE_ISSUE_COMMENT_ISSUE_REQUIRED = "ISSUE_COMMENT_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_COMMENT_BODY_REQUIRED = "ISSUE_COMMENT_BODY_REQUIRED";
    public static final String CODE_ISSUE_COMMENT_ACTOR_REQUIRED = "ISSUE_COMMENT_ACTOR_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    private IssueComment(UUID issueId, String body, UUID actorId) {
        super(body, actorId);
        this.issueId = requireIssueId(issueId);
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

    @Override
    public UUID getTargetId() {
        return issueId;
    }

    @Override
    protected String bodyRequiredCode() {
        return CODE_ISSUE_COMMENT_BODY_REQUIRED;
    }

    @Override
    protected String bodyRequiredMessage() {
        return "댓글 내용은 필수입니다";
    }

    @Override
    protected String actorRequiredCode() {
        return CODE_ISSUE_COMMENT_ACTOR_REQUIRED;
    }

    @Override
    protected String actorRequiredMessage() {
        return "수행자 ID는 필수입니다";
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_COMMENT_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    public UUID getIssueId() {
        return issueId;
    }
}
