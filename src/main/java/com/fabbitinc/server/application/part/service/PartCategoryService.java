package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartItemType;
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

    public PartCategory create(String name, PartItemType itemType, String prefix, String delimiter, int digits) {
        if (partCategoryRepository.existsByName(name)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + name);
        }
        if (partCategoryRepository.existsByPrefix(prefix)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 접두어입니다: " + prefix);
        }

        try {
            PartCategory category = partCategoryRepository.save(
                    PartCategory.create(name, itemType, prefix, delimiter, digits)
            );
            partNumberSequenceRepository.save(PartNumberSequence.createFor(category.getId()));
            return category;
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 카테고리 이름 또는 접두어입니다");
        }
    }

    public PartCategory update(UUID categoryId, String name, PartItemType itemType, String prefix, String delimiter, int digits) {
        PartCategory category = partCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다: " + categoryId));

        partCategoryRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + name);
                });

        if (!category.getPrefix().equals(prefix) && partCategoryRepository.existsByPrefix(prefix)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 접두어입니다: " + prefix);
        }

        try {
            category.changeName(name);
            category.changeItemType(itemType);
            category.changePrefix(prefix);
            category.changeDelimiter(delimiter);
            category.changeDigits(digits);
            return partCategoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 카테고리 이름 또는 접두어입니다");
        }
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
