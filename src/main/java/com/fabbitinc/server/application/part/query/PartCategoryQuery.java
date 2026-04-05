package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.query.result.PartNumberAvailabilityResult;
import com.fabbitinc.server.application.part.query.result.PartCategoryListResult;
import com.fabbitinc.server.application.part.query.result.PartNumberPreviewResult;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartCategoryQuery {

    private static final String PREVIEW_NOTE = "이 번호는 실제 생성 시 변경될 수 있습니다";

    private final CurrentAuthProvider currentAuthProvider;
    private final PartCategoryRepository partCategoryRepository;
    private final PartNumberSequenceRepository partNumberSequenceRepository;
    private final PartRepository partRepository;

    public PartCategoryListResult list(String itemType) {
        currentAuthProvider.getCurrentAuth();
        PartItemType resolvedItemType = resolveItemType(itemType);
        var categories = resolvedItemType == null
                ? partCategoryRepository.findAllByOrderByNameAsc()
                : partCategoryRepository.findAllByItemTypeOrderByNameAsc(resolvedItemType);
        return new PartCategoryListResult(
                categories.stream()
                        .map(category -> new PartCategoryListResult.Item(
                                category.getId(),
                                category.getName(),
                                category.getItemType(),
                                category.getPrefix(),
                                category.getDelimiter(),
                                category.getDigits(),
                                category.formatNumber(1)
                        ))
                        .toList()
        );
    }

    public PartNumberPreviewResult get(UUID categoryId) {
        currentAuthProvider.getCurrentAuth();
        PartCategory category = partCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다: " + categoryId));
        PartNumberSequence sequence = partNumberSequenceRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "채번 시퀀스를 찾을 수 없습니다: " + categoryId));

        int nextValue = sequence.getCurrentValue() + 1;
        if (category.isSequenceExhausted(nextValue)) {
            throw new AppException(ErrorCode.CONFLICT,
                    "시퀀스 최대값에 도달했습니다. 카테고리 '%s'의 자릿수를 확장해 주세요".formatted(category.getName()));
        }

        String previewPartNumber = category.formatNumber(nextValue);
        return new PartNumberPreviewResult(previewPartNumber, PREVIEW_NOTE);
    }

    public PartNumberAvailabilityResult lookup(String partNumber) {
        currentAuthProvider.getCurrentAuth();
        String normalized = partNumber == null ? null : partNumber.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "품번은 필수입니다");
        }
        return new PartNumberAvailabilityResult(
                normalized,
                partRepository.findByPartNumber(normalized).isEmpty()
        );
    }

    private PartItemType resolveItemType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return null;
        }
        try {
            return PartItemType.valueOf(itemType.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 item_type입니다: " + itemType);
        }
    }
}
