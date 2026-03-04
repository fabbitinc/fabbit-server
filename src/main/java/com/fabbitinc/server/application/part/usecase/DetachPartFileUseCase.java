package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DetachPartFileUseCase {

    private final AuthTokenParser authTokenParser;
    private final PartService partService;

    @Transactional
    public void execute(String authorizationHeader, UUID partId, UUID fileId) {
        authTokenParser.requireAuth(authorizationHeader);
        partService.detachFile(partId, fileId);
    }
}
