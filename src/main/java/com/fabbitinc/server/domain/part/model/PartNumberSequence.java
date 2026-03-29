package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채번 카테고리별 시퀀스. 비관적 잠금(SELECT FOR UPDATE)으로 동시성을 제어한다.
 */
@Getter
@Entity
@Table(
        name = "part_number_sequences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_part_number_sequences_category_id", columnNames = "category_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartNumberSequence extends AbstractCreatedEntity {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "current_value", nullable = false)
    private int currentValue;

    private PartNumberSequence(UUID categoryId) {
        super(UuidV7Generator.next());
        this.categoryId = categoryId;
        this.currentValue = 0;
    }

    public static PartNumberSequence createFor(UUID categoryId) {
        return new PartNumberSequence(categoryId);
    }

    /**
     * 시퀀스를 증가시키고 새 값을 반환한다.
     */
    public int nextValue() {
        this.currentValue++;
        return this.currentValue;
    }
}
