package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartCategoryService;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.PartCategoryResult;
import com.fabbitinc.server.domain.part.model.PartCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreatePartCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartCategoryService partCategoryService;

    public PartCategoryResult execute(CreatePartCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartCategory category = partCategoryService.create(
                command.name(),
                command.formatPrefix(),
                command.formatSuffix(),
                command.digits(),
                command.autoNumberingEnabled()
        );
        return new PartCategoryResult(
                category.getId(),
                category.getName(),
                category.getFormatPrefix(),
                category.getFormatSuffix(),
                category.getDigits(),
                category.isAutoNumberingEnabled(),
                category.formatNumber(1)
        );
    }
}
