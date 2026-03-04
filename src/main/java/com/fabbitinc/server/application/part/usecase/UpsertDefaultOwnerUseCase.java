package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.dto.request.PartDefaultOwnerRequest;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpsertDefaultOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    @Transactional
    public UUID execute(PartDefaultOwnerRequest request) {
        currentAuthProvider.getAdminAuth();
        return partService.upsertDefaultOwner(
                request.category(),
                request.defaultOwnerId(),
                request.defaultOwnerTeamId()
        ).getId();
    }
}
