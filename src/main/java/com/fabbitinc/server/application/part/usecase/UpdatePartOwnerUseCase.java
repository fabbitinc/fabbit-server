package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.dto.request.UpdatePartOwnerRequest;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public UUID execute(UUID partId, UpdatePartOwnerRequest request) {
        currentAuthProvider.getCurrentAuth();
        return partService.updateOwner(
                partId,
                request.getOwnerId(),
                request.isOwnerIdSet(),
                request.getOwnerTeamId(),
                request.isOwnerTeamIdSet()
        ).getId();
    }
}
