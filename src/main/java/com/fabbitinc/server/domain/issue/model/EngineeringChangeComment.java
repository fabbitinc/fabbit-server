package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
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
        name = "engineering_change_comments",
        indexes = {
                @Index(name = "ix_engineering_change_comments_engineering_change_id", columnList = "engineering_change_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeComment extends AbstractComment {

    public static final String CODE_ENGINEERING_CHANGE_COMMENT_REQUIRED = "ENGINEERING_CHANGE_COMMENT_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_COMMENT_BODY_REQUIRED = "ENGINEERING_CHANGE_COMMENT_BODY_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_COMMENT_ACTOR_REQUIRED = "ENGINEERING_CHANGE_COMMENT_ACTOR_REQUIRED";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange engineeringChange;

    private EngineeringChangeComment(UUID engineeringChangeId, String body, UUID actorId) {
        super(body, actorId);
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
    }

    public static EngineeringChangeComment write(UUID engineeringChangeId, String body, UUID actorId) {
        return new EngineeringChangeComment(engineeringChangeId, body, actorId);
    }

    public static EngineeringChangeComment write(EngineeringChange engineeringChange, String body, UUID actorId) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_COMMENT_REQUIRED, "변경관리 ID는 필수입니다");
        }
        EngineeringChangeComment comment = new EngineeringChangeComment(engineeringChange.getId(), body, actorId);
        comment.engineeringChange = engineeringChange;
        return comment;
    }

    @Override
    public UUID getTargetId() {
        return engineeringChangeId;
    }

    @Override
    protected String bodyRequiredCode() {
        return CODE_ENGINEERING_CHANGE_COMMENT_BODY_REQUIRED;
    }

    @Override
    protected String bodyRequiredMessage() {
        return "댓글 내용은 필수입니다";
    }

    @Override
    protected String actorRequiredCode() {
        return CODE_ENGINEERING_CHANGE_COMMENT_ACTOR_REQUIRED;
    }

    @Override
    protected String actorRequiredMessage() {
        return "수행자 ID는 필수입니다";
    }

    private UUID requireEngineeringChangeId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_COMMENT_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return value;
    }
}
