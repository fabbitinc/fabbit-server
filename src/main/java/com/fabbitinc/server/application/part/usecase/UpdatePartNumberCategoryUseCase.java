package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartNumberCategoryService;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartNumberCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.PartNumberCategoryResult;
import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartNumberCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartNumberCategoryService partNumberCategoryService;

    public PartNumberCategoryResult execute(UpdatePartNumberCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartNumberCategory category = partNumberCategoryService.update(
                command.categoryId(),
                command.name(),
                command.prefix(),
                command.delimiter(),
                command.digits()
        );
        return new PartNumberCategoryResult(
                category.getId(),
                category.getName(),
                category.getPrefix(),
                category.getDelimiter(),
                category.getDigits(),
                category.formatNumber(1)
        );
    }
}
