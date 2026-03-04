package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "change_requests")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("CHANGE_REQUEST")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeRequest extends Issue {

    private static final String DOMAIN_CODE_INVALID_STATE = "ISSUE_INVALID_STATE";

    @Enumerated(EnumType.STRING)
    @Column(name = "cr_state", nullable = false, length = 20)
    private CrState crState;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by")
    private UUID mergedBy;

    public ChangeRequest(int number, String title, String body, UUID actorId) {
        super(number, title, body, actorId);
        this.crState = CrState.DRAFT;
    }

    public void submit(UUID actorId) {
        if (crState != CrState.DRAFT) {
            throw new DomainException(
                    DOMAIN_CODE_INVALID_STATE,
                    "DRAFT 상태에서만 제출할 수 있습니다 (현재: " + crState + ")"
            );
        }
        this.crState = CrState.SUBMITTED;
        updateTitle(getTitle(), actorId);
    }

    public void merge(Instant now, UUID actorId) {
        if (crState != CrState.SUBMITTED) {
            throw new DomainException(
                    DOMAIN_CODE_INVALID_STATE,
                    "SUBMITTED 상태에서만 반영할 수 있습니다 (현재: " + crState + ")"
            );
        }
        this.crState = CrState.MERGED;
        this.mergedAt = now;
        this.mergedBy = actorId;
        markClosed(now, actorId);
    }

    public void closeCr(Instant now, UUID actorId) {
        if (crState != CrState.DRAFT && crState != CrState.SUBMITTED) {
            throw new DomainException(
                    DOMAIN_CODE_INVALID_STATE,
                    "DRAFT 또는 SUBMITTED 상태에서만 닫을 수 있습니다 (현재: " + crState + ")"
            );
        }
        this.crState = CrState.CLOSED;
        markClosed(now, actorId);
    }

    public void reopenCr(UUID actorId) {
        if (crState != CrState.CLOSED) {
            throw new DomainException(
                    DOMAIN_CODE_INVALID_STATE,
                    "CLOSED 상태에서만 다시 열 수 있습니다 (현재: " + crState + ")"
            );
        }
        this.crState = CrState.SUBMITTED;
        markOpen(actorId);
    }
}
