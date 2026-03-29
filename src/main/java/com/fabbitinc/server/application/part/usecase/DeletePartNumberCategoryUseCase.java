package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartNumberCategoryService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartNumberCategoryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartNumberCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartNumberCategoryService partNumberCategoryService;

    public void execute(DeletePartNumberCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        partNumberCategoryService.delete(command.categoryId());
    }
}
