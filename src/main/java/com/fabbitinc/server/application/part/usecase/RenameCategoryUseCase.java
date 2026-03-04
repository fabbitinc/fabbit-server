package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RenameCategoryUseCase {

    private final AuthTokenParser authTokenParser;
    private final PartService partService;

    @Transactional
    public int execute(String authorizationHeader, String oldName, String newName) {
        authTokenParser.requireAuth(authorizationHeader);
        return partService.renameCategory(oldName, newName);
    }
}
