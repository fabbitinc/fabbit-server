package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartCategoryService {

    private final PartCategoryRepository partCategoryRepository;
    private final PartNumberSequenceRepository partNumberSequenceRepository;
    private final PartRepository partRepository;

    public PartCategory create(String name, String formatPrefix, String formatSuffix, Integer digits, boolean autoNumberingEnabled) {
        if (partCategoryRepository.existsByName(name)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + name);
        }
        int resolvedDigits = resolveDigits(digits, autoNumberingEnabled);
        String normalizedFormatPrefix = normalizeFormatSegment(formatPrefix);
        String normalizedFormatSuffix = normalizeFormatSegment(formatSuffix);
        if (partCategoryRepository.existsByFormatPrefixAndFormatSuffix(normalizedFormatPrefix, normalizedFormatSuffix)) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 존재하는 카테고리 포맷입니다: " + normalizedFormatPrefix + "{number}" + normalizedFormatSuffix
            );
        }

        try {
            PartCategory category = partCategoryRepository.save(
                    PartCategory.create(name, formatPrefix, formatSuffix, resolvedDigits, autoNumberingEnabled)
            );
            partNumberSequenceRepository.save(PartNumberSequence.createFor(category.getId()));
            return category;
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 카테고리 이름 또는 포맷입니다");
        }
    }

    public PartCategory update(
            UUID categoryId,
            String name,
            String formatPrefix,
            String formatSuffix,
            Integer digits,
            boolean autoNumberingEnabled
    ) {
        PartCategory category = partCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다: " + categoryId));

        partCategoryRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + name);
                });

        int resolvedDigits = resolveDigits(digits, autoNumberingEnabled);
        String normalizedFormatPrefix = normalizeFormatSegment(formatPrefix);
        String normalizedFormatSuffix = normalizeFormatSegment(formatSuffix);
        boolean formatChanged = !category.getFormatPrefix().equals(normalizedFormatPrefix)
                || !category.getFormatSuffix().equals(normalizedFormatSuffix);
        if (formatChanged && partCategoryRepository.existsByFormatPrefixAndFormatSuffix(normalizedFormatPrefix, normalizedFormatSuffix)) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 존재하는 카테고리 포맷입니다: " + normalizedFormatPrefix + "{number}" + normalizedFormatSuffix
            );
        }

        try {
            category.changeName(name);
            category.changeFormatPrefix(formatPrefix);
            category.changeFormatSuffix(formatSuffix);
            category.changeDigits(resolvedDigits);
            category.changeAutoNumberingEnabled(autoNumberingEnabled);
            return partCategoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 카테고리 이름 또는 포맷입니다");
        }
    }

    private int resolveDigits(Integer digits, boolean autoNumberingEnabled) {
        if (autoNumberingEnabled) {
            if (digits == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "자동채번 카테고리에서는 digits가 필수입니다");
            }
            if (digits < 1 || digits > 10) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "digits는 1 이상 10 이하여야 합니다");
            }
            return digits;
        }
        return digits == null ? 1 : digits;
    }

    private String normalizeFormatSegment(String value) {
        return value == null ? "" : value.trim();
    }

    public void delete(UUID categoryId) {
        PartCategory category = partCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다: " + categoryId));

        long referencedCount = partRepository.countByCategoryId(categoryId);
        if (referencedCount > 0) {
            throw new AppException(ErrorCode.CONFLICT, "사용 중인 카테고리는 삭제할 수 없습니다: " + category.getName());
        }

        try {
            partNumberSequenceRepository.findByCategoryIdForUpdate(categoryId)
                    .ifPresent(partNumberSequenceRepository::delete);
            partCategoryRepository.delete(category);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "사용 중인 카테고리는 삭제할 수 없습니다: " + category.getName());
        }
    }
}
