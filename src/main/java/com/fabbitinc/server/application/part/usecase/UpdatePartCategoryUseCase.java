package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartCategoryService;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.PartCategoryResult;
import com.fabbitinc.server.domain.part.model.PartCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartCategoryService partCategoryService;

    public PartCategoryResult execute(UpdatePartCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartCategory category = partCategoryService.update(
                command.categoryId(),
                command.name(),
                command.prefix(),
                command.delimiter(),
                command.digits()
        );
        return new PartCategoryResult(
                category.getId(),
                category.getName(),
                category.getPrefix(),
                category.getDelimiter(),
                category.getDigits(),
                category.formatNumber(1)
        );
    }
}
