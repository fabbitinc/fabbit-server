package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부품 카테고리 마스터. 카테고리별 채번 규칙을 정의한다.
 */
@Getter
@Entity
@Table(
        name = "part_number_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_part_number_categories_name", columnNames = "name"),
                @UniqueConstraint(name = "uq_part_number_categories_prefix", columnNames = "prefix")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartCategory extends AbstractAuditableEntity {

    public static final String CODE_NAME_REQUIRED = "PART_CATEGORY_NAME_REQUIRED";
    public static final String CODE_PREFIX_REQUIRED = "PART_CATEGORY_PREFIX_REQUIRED";
    public static final String CODE_DIGITS_INVALID = "PART_CATEGORY_DIGITS_INVALID";

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PREFIX_LENGTH = 20;
    private static final int MAX_DELIMITER_LENGTH = 5;
    private static final int MIN_DIGITS = 1;
    private static final int MAX_DIGITS = 10;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "prefix", nullable = false, length = MAX_PREFIX_LENGTH)
    private String prefix;

    @Column(name = "delimiter", nullable = false, length = MAX_DELIMITER_LENGTH)
    private String delimiter;

    @Column(name = "digits", nullable = false)
    private int digits;

    private PartCategory(String name, String prefix, String delimiter, int digits) {
        super(UuidV7Generator.next());
        this.name = validateName(name);
        this.prefix = validatePrefix(prefix);
        this.delimiter = delimiter == null ? "-" : delimiter;
        this.digits = validateDigits(digits);
    }

    public static PartCategory create(String name, String prefix, String delimiter, int digits) {
        return new PartCategory(name, prefix, delimiter, digits);
    }

    /**
     * 시퀀스 값을 채번 규칙에 따라 포맷한다. 예: prefix="PCB", delimiter="-", digits=4, seq=1 → "PCB-0001"
     */
    public String formatNumber(int sequenceValue) {
        return prefix + delimiter + String.format("%0" + digits + "d", sequenceValue);
    }

    /**
     * 시퀀스 값이 자릿수 범위 내인지 확인한다.
     */
    public boolean isSequenceExhausted(int sequenceValue) {
        int maxValue = (int) Math.pow(10, digits) - 1;
        return sequenceValue > maxValue;
    }

    public void changeName(String name) {
        this.name = validateName(name);
    }

    public void changePrefix(String prefix) {
        this.prefix = validatePrefix(prefix);
    }

    public void changeDelimiter(String delimiter) {
        this.delimiter = delimiter == null ? "-" : delimiter;
    }

    public void changeDigits(int digits) {
        this.digits = validateDigits(digits);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException(CODE_NAME_REQUIRED, "채번 카테고리 이름은 필수입니다");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_NAME_REQUIRED, "채번 카테고리 이름은 %d자 이하여야 합니다".formatted(MAX_NAME_LENGTH));
        }
        return trimmed;
    }

    private String validatePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new DomainException(CODE_PREFIX_REQUIRED, "카테고리 접두어는 필수입니다");
        }
        String trimmed = prefix.trim();
        if (trimmed.length() > MAX_PREFIX_LENGTH) {
            throw new DomainException(CODE_PREFIX_REQUIRED, "카테고리 접두어는 %d자 이하여야 합니다".formatted(MAX_PREFIX_LENGTH));
        }
        return trimmed;
    }

    private int validateDigits(int digits) {
        if (digits < MIN_DIGITS || digits > MAX_DIGITS) {
            throw new DomainException(CODE_DIGITS_INVALID, "자릿수는 %d~%d 사이여야 합니다".formatted(MIN_DIGITS, MAX_DIGITS));
        }
        return digits;
    }
}
