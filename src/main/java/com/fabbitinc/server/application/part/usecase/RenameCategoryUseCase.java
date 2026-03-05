package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RenameCategoryUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public int execute(String oldName, String newName) {
        currentAuthProvider.getCurrentAuth();
        return partService.renameCategory(oldName, newName);
    }
}
