package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "issue_number_sequences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueNumberSequence extends AbstractIdEntity {

    public static final String CODE_ISSUE_NUMBER_INVALID = "ISSUE_NUMBER_INVALID";
    public static final String CODE_ISSUE_NUMBER_EXHAUSTED = "ISSUE_NUMBER_EXHAUSTED";

    @Column(name = "next_number", nullable = false)
    private int nextNumber;

    private IssueNumberSequence(UUID id, int nextNumber) {
        super(id);
        this.nextNumber = requireValidNextNumber(nextNumber);
    }

    public static IssueNumberSequence initialize(UUID id, int nextNumber) {
        return new IssueNumberSequence(id, nextNumber);
    }

    public int allocateNextNumber() {
        int currentNumber = requireValidNextNumber(nextNumber);
        if (currentNumber == Integer.MAX_VALUE) {
            throw new DomainException(CODE_ISSUE_NUMBER_EXHAUSTED, "이슈 번호를 더 이상 발급할 수 없습니다");
        }
        nextNumber = currentNumber + 1;
        return currentNumber;
    }

    private int requireValidNextNumber(int value) {
        if (value < 1) {
            throw new DomainException(CODE_ISSUE_NUMBER_INVALID, "다음 이슈 번호는 1 이상이어야 합니다");
        }
        return value;
    }
}
