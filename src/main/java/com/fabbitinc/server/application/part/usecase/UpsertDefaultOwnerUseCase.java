package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.part.dto.request.PartDefaultOwnerRequest;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpsertDefaultOwnerUseCase {

    private final AuthTokenParser authTokenParser;
    private final PartService partService;

    @Transactional
    public UUID execute(String authorizationHeader, PartDefaultOwnerRequest request) {
        authTokenParser.requireAdmin(authorizationHeader);
        return partService.upsertDefaultOwner(
                request.category(),
                request.defaultOwnerId(),
                request.defaultOwnerTeamId()
        ).getId();
    }
}
