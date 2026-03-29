package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.usecase.command.RenameCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.RenameCategoryResult;
import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import com.fabbitinc.server.domain.part.repository.PartNumberCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RenameCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartNumberCategoryRepository partNumberCategoryRepository;
    private final PartRepository partRepository;

    public RenameCategoryResult execute(RenameCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();

        String oldName = normalizeRequired(command.oldName(), "기존");
        String newName = normalizeRequired(command.newName(), "변경");

        if (oldName.equals(newName)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 전후 카테고리 이름이 동일합니다");
        }

        PartNumberCategory category = partNumberCategoryRepository.findByName(oldName)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "카테고리 '" + oldName + "'을(를) 찾을 수 없습니다"
                ));

        if (partNumberCategoryRepository.existsByName(newName)) {
            throw new AppException(ErrorCode.CONFLICT, "카테고리 '" + newName + "'이(가) 이미 존재합니다");
        }

        category.changeName(newName);
        long updatedCount = partRepository.countByNumberingCategoryId(category.getId());
        return new RenameCategoryResult((int) updatedCount);
    }

    private String normalizeRequired(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, label + " 카테고리는 비어 있을 수 없습니다");
        }
        return raw.trim();
    }
}
