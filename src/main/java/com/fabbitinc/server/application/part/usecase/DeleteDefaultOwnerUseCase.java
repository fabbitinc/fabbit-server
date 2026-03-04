package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteDefaultOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    @Transactional
    public void execute(String category) {
        currentAuthProvider.getAdminAuth();
        partService.deleteDefaultOwner(category);
    }
}
