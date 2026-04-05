package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartCategoryService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartCategoryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartCategoryService partCategoryService;

    public void execute(DeletePartCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        partCategoryService.delete(command.categoryId());
    }
}
