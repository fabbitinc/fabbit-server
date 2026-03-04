package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteDefaultOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void execute(String category) {
        currentAuthProvider.getCurrentAuth();
        partService.deleteDefaultOwner(category);
    }
}
