package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.part.dto.request.UpdatePartOwnerRequest;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdatePartOwnerUseCase {

    private final AuthTokenParser authTokenParser;
    private final PartService partService;

    @Transactional
    public UUID execute(String authorizationHeader, UUID partId, UpdatePartOwnerRequest request) {
        authTokenParser.requireAuth(authorizationHeader);
        return partService.updateOwner(
                partId,
                request.getOwnerId(),
                request.isOwnerIdSet(),
                request.getOwnerTeamId(),
                request.isOwnerTeamIdSet()
        ).getId();
    }
}
