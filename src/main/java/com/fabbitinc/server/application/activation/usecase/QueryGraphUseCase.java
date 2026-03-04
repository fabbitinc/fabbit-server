package com.fabbitinc.server.application.activation.usecase;

import com.fabbitinc.server.application.activation.dto.response.QueryResponse;
import com.fabbitinc.server.application.activation.service.ActivationService;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QueryGraphUseCase {

    private final AuthTokenParser authTokenParser;
    private final ActivationService activationService;

    @Transactional(readOnly = true)
    public QueryResponse execute(String authorizationHeader, String question) {
        authTokenParser.requireAuth(authorizationHeader);
        return activationService.queryGraph(question);
    }
}
