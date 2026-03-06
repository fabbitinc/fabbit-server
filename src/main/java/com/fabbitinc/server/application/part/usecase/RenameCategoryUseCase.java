package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.RenameCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.RenameCategoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RenameCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public RenameCategoryResult execute(RenameCategoryCommand command) {
        currentAuthProvider.getCurrentAuth();
        return new RenameCategoryResult(
                partService.renameCategory(command.oldName(), command.newName())
        );
    }
}
