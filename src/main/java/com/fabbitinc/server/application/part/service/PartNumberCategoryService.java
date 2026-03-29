package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartNumberCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartNumberCategoryService {

    private final PartNumberCategoryRepository partNumberCategoryRepository;
    private final PartNumberSequenceRepository partNumberSequenceRepository;
    private final PartRepository partRepository;

    public PartNumberCategory create(String name, String prefix, String delimiter, int digits) {
        if (partNumberCategoryRepository.existsByName(name)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 채번 카테고리 이름입니다: " + name);
        }
        if (partNumberCategoryRepository.existsByPrefix(prefix)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 채번 접두어입니다: " + prefix);
        }

        try {
            PartNumberCategory category = partNumberCategoryRepository.save(
                    PartNumberCategory.create(name, prefix, delimiter, digits)
            );
            partNumberSequenceRepository.save(PartNumberSequence.createFor(category.getId()));
            return category;
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 채번 카테고리 이름 또는 접두어입니다");
        }
    }

    public PartNumberCategory update(UUID categoryId, String name, String prefix, String delimiter, int digits) {
        PartNumberCategory category = partNumberCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "채번 카테고리를 찾을 수 없습니다: " + categoryId));

        partNumberCategoryRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 채번 카테고리 이름입니다: " + name);
                });

        if (!category.getPrefix().equals(prefix) && partNumberCategoryRepository.existsByPrefix(prefix)) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 채번 접두어입니다: " + prefix);
        }

        try {
            category.changeName(name);
            category.changePrefix(prefix);
            category.changeDelimiter(delimiter);
            category.changeDigits(digits);
            return partNumberCategoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "중복된 채번 카테고리 이름 또는 접두어입니다");
        }
    }

    public void delete(UUID categoryId) {
        PartNumberCategory category = partNumberCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "채번 카테고리를 찾을 수 없습니다: " + categoryId));

        long referencedCount = partRepository.countByNumberingCategoryId(categoryId);
        if (referencedCount > 0) {
            throw new AppException(ErrorCode.CONFLICT, "사용 중인 채번 카테고리는 삭제할 수 없습니다: " + category.getName());
        }

        partNumberSequenceRepository.findByCategoryIdForUpdate(categoryId)
                .ifPresent(partNumberSequenceRepository::delete);
        partNumberCategoryRepository.delete(category);
    }
}
