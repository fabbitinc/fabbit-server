package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.UpsertDefaultOwnerCommand;
import com.fabbitinc.server.application.part.usecase.result.UpsertDefaultOwnerResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpsertDefaultOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    @PreAuthorize("hasRole('ADMIN')")
    public UpsertDefaultOwnerResult execute(UpsertDefaultOwnerCommand command) {
        currentAuthProvider.getCurrentAuth();
        return new UpsertDefaultOwnerResult(
                partService.upsertDefaultOwner(
                        command.category(),
                        command.defaultOwnerId(),
                        command.defaultOwnerTeamId()
                ).getId()
        );
    }
}
